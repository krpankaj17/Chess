"use client";

import { useState, useEffect, useRef } from "react";
import { Client } from "@stomp/stompjs";

type Piece = {
  type: "PAWN" | "ROOK" | "KNIGHT" | "BISHOP" | "QUEEN" | "KING";
  color: "WHITE" | "BLACK";
} | null;

type UserProfile = {
  username: string;
  eloRating: number;
  token: string;
};

type MatchData = {
  gameId: string;
  whitePlayer: string;
  blackPlayer: string;
};

type GameStateMessage = {
  gameId: string;
  currentTurn: "WHITE" | "BLACK";
  status: string;
  fromRow: number;
  fromCol: number;
  toRow: number;
  toCol: number;
  movedPiece: string;
  capturedPiece?: string;
  promotionPiece?: string;
  moveType?: "NORMAL" | "CAPTURE" | "CASTLING" | "EN_PASSANT" | "PROMOTION";
  rookFromRow?: number;
  rookFromCol?: number;
  rookToRow?: number;
  rookToCol?: number;
  capturedSquareRow?: number;
  capturedSquareCol?: number;
  check: boolean;
  checkedKingColor?: string;
  gameOver: boolean;
  winner?: string;
  whiteElo?: number;
  blackElo?: number;
};

type MoveHistoryItem = {
  moveNumber: number;
  white: string;
  black?: string;
};

const UNICODE_PIECES: Record<string, string> = {
  WHITE_KING: "♔",
  WHITE_QUEEN: "♕",
  WHITE_ROOK: "♖",
  WHITE_BISHOP: "♗",
  WHITE_KNIGHT: "♘",
  WHITE_PAWN: "♙",
  BLACK_KING: "♚",
  BLACK_QUEEN: "♛",
  BLACK_ROOK: "♜",
  BLACK_BISHOP: "♝",
  BLACK_KNIGHT: "♞",
  BLACK_PAWN: "♟",
};

const PIECE_VALUES: Record<string, number> = {
  PAWN: 1,
  KNIGHT: 3,
  BISHOP: 3,
  ROOK: 5,
  QUEEN: 9,
  KING: 0,
};

const PIECE_LETTERS: Record<string, string> = {
  KING: "K",
  QUEEN: "Q",
  ROOK: "R",
  BISHOP: "B",
  KNIGHT: "N",
  PAWN: "",
};

const API_BASE = "http://localhost:8080";
const WS_BASE = "ws://localhost:8080/ws";

function playSound(type: "move" | "capture" | "check" | "castle" | "gameover") {
  try {
    const AudioCtx = window.AudioContext || (window as any).webkitAudioContext;
    if (!AudioCtx) return;
    const ctx = new AudioCtx();
    const osc = ctx.createOscillator();
    const gain = ctx.createGain();
    osc.connect(gain);
    gain.connect(ctx.destination);

    if (type === "move") {
      osc.type = "sine";
      osc.frequency.setValueAtTime(320, ctx.currentTime);
      osc.frequency.exponentialRampToValueAtTime(140, ctx.currentTime + 0.08);
      gain.gain.setValueAtTime(0.25, ctx.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.01, ctx.currentTime + 0.08);
      osc.start();
      osc.stop(ctx.currentTime + 0.08);
    } else if (type === "capture") {
      osc.type = "triangle";
      osc.frequency.setValueAtTime(540, ctx.currentTime);
      osc.frequency.exponentialRampToValueAtTime(110, ctx.currentTime + 0.12);
      gain.gain.setValueAtTime(0.4, ctx.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.01, ctx.currentTime + 0.12);
      osc.start();
      osc.stop(ctx.currentTime + 0.12);
    } else if (type === "castle") {
      // Dual-click sound for castling
      osc.type = "sine";
      osc.frequency.setValueAtTime(380, ctx.currentTime);
      osc.frequency.setValueAtTime(280, ctx.currentTime + 0.06);
      gain.gain.setValueAtTime(0.3, ctx.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.01, ctx.currentTime + 0.14);
      osc.start();
      osc.stop(ctx.currentTime + 0.14);
    } else if (type === "check") {
      osc.type = "sawtooth";
      osc.frequency.setValueAtTime(480, ctx.currentTime);
      osc.frequency.setValueAtTime(640, ctx.currentTime + 0.08);
      gain.gain.setValueAtTime(0.35, ctx.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.01, ctx.currentTime + 0.22);
      osc.start();
      osc.stop(ctx.currentTime + 0.22);
    } else if (type === "gameover") {
      osc.type = "sine";
      osc.frequency.setValueAtTime(260, ctx.currentTime);
      osc.frequency.setValueAtTime(390, ctx.currentTime + 0.12);
      osc.frequency.setValueAtTime(520, ctx.currentTime + 0.24);
      gain.gain.setValueAtTime(0.4, ctx.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.01, ctx.currentTime + 0.4);
      osc.start();
      osc.stop(ctx.currentTime + 0.4);
    }
  } catch {}
}

function createInitialBoard(): Piece[][] {
  const b: Piece[][] = Array(8).fill(null).map(() => Array(8).fill(null));
  const backRank: ("ROOK" | "KNIGHT" | "BISHOP" | "QUEEN" | "KING" | "BISHOP" | "KNIGHT" | "ROOK")[] = [
    "ROOK", "KNIGHT", "BISHOP", "QUEEN", "KING", "BISHOP", "KNIGHT", "ROOK"
  ];
  backRank.forEach((type, col) => {
    b[0][col] = { type, color: "WHITE" };
  });
  for (let c = 0; c < 8; c++) {
    b[1][c] = { type: "PAWN", color: "WHITE" };
  }
  backRank.forEach((type, col) => {
    b[7][col] = { type, color: "BLACK" };
  });
  for (let c = 0; c < 8; c++) {
    b[6][c] = { type: "PAWN", color: "BLACK" };
  }
  return b;
}

function calculateClientLegalMoves(
  board: Piece[][],
  pos: { r: number; c: number },
  lastMove: { from: { r: number; c: number }; to: { r: number; c: number } } | null
): Array<{ r: number; c: number; isCapture: boolean }> {
  const piece = board[pos.r]?.[pos.c];
  if (!piece) return [];
  const moves: Array<{ r: number; c: number; isCapture: boolean }> = [];
  const color = piece.color;
  const isWhite = color === "WHITE";

  const isInside = (r: number, c: number) => r >= 0 && r < 8 && c >= 0 && c < 8;

  if (piece.type === "PAWN") {
    const dir = isWhite ? 1 : -1;
    const startRow = isWhite ? 1 : 6;

    // 1 step forward
    const f1 = pos.r + dir;
    if (isInside(f1, pos.c) && !board[f1][pos.c]) {
      moves.push({ r: f1, c: pos.c, isCapture: false });

      // 2 steps forward
      const f2 = pos.r + 2 * dir;
      if (pos.r === startRow && isInside(f2, pos.c) && !board[f2][pos.c]) {
        moves.push({ r: f2, c: pos.c, isCapture: false });
      }
    }

    // Diagonals & en passant
    [-1, 1].forEach((dc) => {
      const targetC = pos.c + dc;
      const targetR = pos.r + dir;
      if (isInside(targetR, targetC)) {
        const destPiece = board[targetR][targetC];
        if (destPiece && destPiece.color !== color) {
          moves.push({ r: targetR, c: targetC, isCapture: true });
        }
      }

      // En Passant check
      if (
        lastMove &&
        Math.abs(lastMove.from.r - lastMove.to.r) === 2 &&
        lastMove.to.r === pos.r &&
        lastMove.to.c === targetC
      ) {
        const enemyPawn = board[lastMove.to.r]?.[lastMove.to.c];
        if (enemyPawn && enemyPawn.type === "PAWN" && enemyPawn.color !== color) {
          // Empty diagonal square
          moves.push({ r: pos.r + dir, c: targetC, isCapture: true });
          // Enemy pawn square itself
          moves.push({ r: pos.r, c: targetC, isCapture: true });
        }
      }
    });
  } else if (piece.type === "KNIGHT") {
    const jumps = [
      [-2, -1], [-2, 1], [-1, -2], [-1, 2],
      [1, -2], [1, 2], [2, -1], [2, 1]
    ];
    for (const [dr, dc] of jumps) {
      const nr = pos.r + dr;
      const nc = pos.c + dc;
      if (isInside(nr, nc)) {
        const target = board[nr][nc];
        if (!target) {
          moves.push({ r: nr, c: nc, isCapture: false });
        } else if (target.color !== color) {
          moves.push({ r: nr, c: nc, isCapture: true });
        }
      }
    }
  } else if (piece.type === "BISHOP" || piece.type === "ROOK" || piece.type === "QUEEN") {
    const directions: number[][] = [];
    if (piece.type === "BISHOP" || piece.type === "QUEEN") {
      directions.push([-1, -1], [-1, 1], [1, -1], [1, 1]);
    }
    if (piece.type === "ROOK" || piece.type === "QUEEN") {
      directions.push([-1, 0], [1, 0], [0, -1], [0, 1]);
    }

    for (const [dr, dc] of directions) {
      let step = 1;
      while (true) {
        const nr = pos.r + dr * step;
        const nc = pos.c + dc * step;
        if (!isInside(nr, nc)) break;
        const target = board[nr][nc];
        if (!target) {
          moves.push({ r: nr, c: nc, isCapture: false });
        } else {
          if (target.color !== color) {
            moves.push({ r: nr, c: nc, isCapture: true });
          }
          break;
        }
        step++;
      }
    }
  } else if (piece.type === "KING") {
    const steps = [
      [-1, -1], [-1, 0], [-1, 1],
      [0, -1],           [0, 1],
      [1, -1],  [1, 0],  [1, 1]
    ];
    for (const [dr, dc] of steps) {
      const nr = pos.r + dr;
      const nc = pos.c + dc;
      if (isInside(nr, nc)) {
        const target = board[nr][nc];
        if (!target) {
          moves.push({ r: nr, c: nc, isCapture: false });
        } else if (target.color !== color) {
          moves.push({ r: nr, c: nc, isCapture: true });
        }
      }
    }
    // Castling targets: (pos.r, 6) Kingside, (pos.r, 2) Queenside
    if (pos.c === 4) {
      // Kingside: cols 5 and 6 empty
      if (!board[pos.r][5] && !board[pos.r][6]) {
        const rook = board[pos.r][7];
        if (rook && rook.type === "ROOK" && rook.color === color) {
          moves.push({ r: pos.r, c: 6, isCapture: false });
        }
      }
      // Queenside: cols 1, 2, 3 empty
      if (!board[pos.r][1] && !board[pos.r][2] && !board[pos.r][3]) {
        const rook = board[pos.r][0];
        if (rook && rook.type === "ROOK" && rook.color === color) {
          moves.push({ r: pos.r, c: 2, isCapture: false });
        }
      }
    }
  }

  return moves;
}

export default function Home() {
  const [user, setUser] = useState<UserProfile | null>(null);
  const [authTab, setAuthTab] = useState<"login" | "register">("login");
  const [usernameInput, setUsernameInput] = useState("");
  const [passwordInput, setPasswordInput] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [emailInput, setEmailInput] = useState("");
  const [authError, setAuthError] = useState("");

  const [searching, setSearching] = useState(false);
  const [matchData, setMatchData] = useState<MatchData | null>(null);

  const [board, setBoard] = useState<Piece[][]>(createInitialBoard);
  const [selectedSquare, setSelectedSquare] = useState<{ r: number; c: number } | null>(null);
  const [lastMove, setLastMove] = useState<{ from: { r: number; c: number }; to: { r: number; c: number } } | null>(null);
  const [currentTurn, setCurrentTurn] = useState<"WHITE" | "BLACK">("WHITE");
  const [gameStatus, setGameStatus] = useState("IN_PROGRESS");
  const [checkedKingColor, setCheckedKingColor] = useState<string | null>(null);
  const [gameOver, setGameOver] = useState(false);
  const [winner, setWinner] = useState<string | null>(null);
  const [pendingPromotion, setPendingPromotion] = useState<{ from: { r: number; c: number }; to: { r: number; c: number } } | null>(null);

  // Move history log & captured pieces
  const [moveHistory, setMoveHistory] = useState<MoveHistoryItem[]>([]);
  const [capturedWhite, setCapturedWhite] = useState<string[]>([]); // pieces captured by black (were white)
  const [capturedBlack, setCapturedBlack] = useState<string[]>([]); // pieces captured by white (were black)

  const stompClientRef = useRef<Client | null>(null);
  const pollIntervalRef = useRef<NodeJS.Timeout | null>(null);
  const notationBoxRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    const savedToken = localStorage.getItem("token");
    const savedUser = localStorage.getItem("username");
    if (savedToken && savedUser) {
      fetch(`${API_BASE}/api/auth/me`, {
        headers: { Authorization: `Bearer ${savedToken}` }
      })
        .then(res => res.ok ? res.json() : Promise.reject())
        .then(data => {
          const u = { username: data.username, eloRating: data.eloRating, token: savedToken };
          setUser(u);
          checkActiveGame(savedToken);
        })
        .catch(() => {
          localStorage.clear();
          setUser(null);
        });
    }
  }, []);

  useEffect(() => {
    if (notationBoxRef.current) {
      notationBoxRef.current.scrollTop = notationBoxRef.current.scrollHeight;
    }
  }, [moveHistory]);

  const checkActiveGame = async (token: string) => {
    try {
      const res = await fetch(`${API_BASE}/api/matchmaking/current`, {
        headers: { Authorization: `Bearer ${token}` }
      });
      if (res.status === 200) {
        const snapshot = await res.json();
        if (snapshot && snapshot.board) {
          resumeGame(snapshot, token);
        }
      }
    } catch {}
  };

  const resumeGame = (snapshot: any, token: string) => {
    setMatchData({
      gameId: snapshot.gameId,
      whitePlayer: snapshot.whitePlayer,
      blackPlayer: snapshot.blackPlayer
    });
    setBoard(snapshot.board);
    setSelectedSquare(null);
    setCurrentTurn(snapshot.currentTurn);
    setGameStatus(snapshot.status);
    setCheckedKingColor(snapshot.check ? (snapshot.checkedKingColor || snapshot.currentTurn) : null);
    setGameOver(snapshot.gameOver);
    setWinner(snapshot.winner);
    setPendingPromotion(null);
    setLastMove(null);
    setMoveHistory([]);
    setCapturedWhite([]);
    setCapturedBlack([]);

    connectWebSocket(snapshot.gameId, token, snapshot.whitePlayer);
  };

  const handleAuth = async (e: React.FormEvent) => {
    e.preventDefault();
    setAuthError("");
    const endpoint = authTab === "login" ? "/api/auth/login" : "/api/auth/register";
    const body = authTab === "login"
      ? { username: usernameInput, password: passwordInput }
      : { username: usernameInput, password: passwordInput, email: emailInput };

    try {
      const res = await fetch(`${API_BASE}${endpoint}`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(body)
      });
      if (!res.ok) {
        throw new Error("Authentication failed");
      }
      const data = await res.json();
      localStorage.setItem("token", data.token);
      localStorage.setItem("username", data.username);
      setUser({ username: data.username, eloRating: data.eloRating, token: data.token });
      checkActiveGame(data.token);
    } catch (err: any) {
      setAuthError(err.message || "Error");
    }
  };

  const handleLogout = () => {
    localStorage.clear();
    setUser(null);
    cancelMatchmaking();
  };

  const startMatchmaking = async () => {
    if (!user) return;
    setSearching(true);
    try {
      const res = await fetch(`${API_BASE}/api/matchmaking/find`, {
        method: "POST",
        headers: { Authorization: `Bearer ${user.token}` }
      });
      const data = await res.json();
      if (data.matched) {
        setSearching(false);
        checkActiveGame(user.token);
      } else {
        pollIntervalRef.current = setInterval(pollMatchStatus, 1000);
      }
    } catch {
      setSearching(false);
    }
  };

  const pollMatchStatus = async () => {
    if (!user) return;
    try {
      const res = await fetch(`${API_BASE}/api/matchmaking/status`, {
        headers: { Authorization: `Bearer ${user.token}` }
      });
      const data = await res.json();
      if (data.matched) {
        if (pollIntervalRef.current) clearInterval(pollIntervalRef.current);
        setSearching(false);
        checkActiveGame(user.token);
      }
    } catch {
      cancelMatchmaking();
    }
  };

  const cancelMatchmaking = async () => {
    if (pollIntervalRef.current) clearInterval(pollIntervalRef.current);
    setSearching(false);
    if (!user) return;
    try {
      await fetch(`${API_BASE}/api/matchmaking/cancel`, {
        method: "POST",
        headers: { Authorization: `Bearer ${user.token}` }
      });
    } catch {}
  };

  const formatAlgebraicNotation = (state: GameStateMessage): string => {
    if (state.moveType === "CASTLING") {
      return state.toCol === 6 ? "O-O" : "O-O-O";
    }

    const fromFile = String.fromCharCode(97 + state.fromCol);
    const toSquare = `${String.fromCharCode(97 + state.toCol)}${state.toRow + 1}`;
    const pieceChar = PIECE_LETTERS[state.movedPiece] ?? "";
    const isCapture = Boolean(state.capturedPiece || state.moveType === "EN_PASSANT");

    let moveText = "";
    if (state.movedPiece === "PAWN") {
      if (isCapture) {
        moveText = `${fromFile}x${toSquare}`;
      } else {
        moveText = toSquare;
      }
      if (state.promotionPiece) {
        const promoChar = PIECE_LETTERS[state.promotionPiece] ?? "Q";
        moveText += `=${promoChar}`;
      }
      if (state.moveType === "EN_PASSANT") {
        moveText += " e.p.";
      }
    } else {
      moveText = `${pieceChar}${isCapture ? "x" : ""}${toSquare}`;
    }

    if (state.status === "CHECKMATE") {
      moveText += "#";
    } else if (state.check) {
      moveText += "+";
    }

    return moveText;
  };

  const connectWebSocket = (gameId: string, token: string, whitePlayer: string) => {
    if (stompClientRef.current) {
      stompClientRef.current.deactivate();
      stompClientRef.current = null;
    }

    const client = new Client({
      brokerURL: WS_BASE,
      connectHeaders: { Authorization: `Bearer ${token}` },
      onConnect: () => {
        client.subscribe(`/topic/game/${gameId}`, (message) => {
          const state: GameStateMessage = JSON.parse(message.body);

          // Update board state with special moves support
          setBoard(prev => {
            const next = prev.map(row => [...row]);
            let piece = next[state.fromRow]?.[state.fromCol];
            if (!piece && state.movedPiece) {
              piece = {
                type: state.movedPiece as any,
                color: (state.currentTurn === "WHITE" ? "BLACK" : "WHITE")
              };
            }
            next[state.fromRow][state.fromCol] = null;

            // Handle En Passant pawn removal
            if (state.moveType === "EN_PASSANT") {
              const capR = state.capturedSquareRow ?? state.fromRow;
              const capC = state.capturedSquareCol ?? state.toCol;
              next[capR][capC] = null;
            }

            // Handle Castling Rook move
            if (state.moveType === "CASTLING") {
              const rFromR = state.rookFromRow ?? state.fromRow;
              const rFromC = state.rookFromCol ?? (state.toCol === 6 ? 7 : 0);
              const rToR = state.rookToRow ?? state.toRow;
              const rToC = state.rookToCol ?? (state.toCol === 6 ? 5 : 3);
              const rook = next[rFromR]?.[rFromC] || {
                type: "ROOK",
                color: (state.currentTurn === "WHITE" ? "BLACK" : "WHITE")
              };
              next[rFromR][rFromC] = null;
              next[rToR][rToC] = rook;
            }

            // Handle Promotion
            if (state.promotionPiece) {
              const promoType = state.promotionPiece as "QUEEN" | "ROOK" | "BISHOP" | "KNIGHT";
              next[state.toRow][state.toCol] = { type: promoType, color: piece ? piece.color : "WHITE" };
            } else {
              next[state.toRow][state.toCol] = piece;
            }
            return next;
          });

          // Set last move squares for visual highlights
          setLastMove({
            from: { r: state.fromRow, c: state.fromCol },
            to: { r: state.toRow, c: state.toCol }
          });

          // Update captured pieces
          if (state.capturedPiece) {
            if (state.currentTurn === "WHITE") {
              // Current turn is now WHITE, meaning BLACK just moved and captured
              setCapturedWhite(prev => [...prev, state.capturedPiece!]);
            } else {
              // White just moved and captured
              setCapturedBlack(prev => [...prev, state.capturedPiece!]);
            }
          } else if (state.moveType === "EN_PASSANT") {
            if (state.currentTurn === "WHITE") {
              setCapturedWhite(prev => [...prev, "PAWN"]);
            } else {
              setCapturedBlack(prev => [...prev, "PAWN"]);
            }
          }

          // Update algebraic move notation history
          const notation = formatAlgebraicNotation(state);
          setMoveHistory(prev => {
            const isWhiteMove = state.currentTurn === "BLACK"; // Just finished White's move
            if (isWhiteMove) {
              return [...prev, { moveNumber: prev.length + 1, white: notation }];
            } else if (prev.length > 0) {
              const last = { ...prev[prev.length - 1], black: notation };
              return [...prev.slice(0, -1), last];
            } else {
              return [{ moveNumber: 1, white: "...", black: notation }];
            }
          });

          setCurrentTurn(state.currentTurn);
          setGameStatus(state.status);
          setCheckedKingColor(state.check ? (state.checkedKingColor || state.currentTurn) : null);

          // Play appropriate audio cue
          if (state.gameOver) {
            playSound("gameover");
            setGameOver(true);
            setWinner(state.winner || null);
            setUser(prev => {
              if (!prev) return null;
              const isWhite = prev.username === whitePlayer;
              const newElo = isWhite ? state.whiteElo : state.blackElo;
              return newElo ? { ...prev, eloRating: newElo } : prev;
            });
          } else if (state.check) {
            playSound("check");
          } else if (state.moveType === "CASTLING") {
            playSound("castle");
          } else if (state.capturedPiece || state.moveType === "EN_PASSANT") {
            playSound("capture");
          } else {
            playSound("move");
          }
        });
      }
    });

    client.activate();
    stompClientRef.current = client;
  };

  const handleSquareClick = (r: number, c: number) => {
    if (!matchData || gameOver || pendingPromotion) return;

    const myColor = user?.username === matchData.whitePlayer ? "WHITE" : "BLACK";
    if (currentTurn !== myColor) return;

    if (!selectedSquare) {
      const piece = board[r][c];
      if (piece && piece.color === myColor) {
        setSelectedSquare({ r, c });
      }
      return;
    }

    if (selectedSquare.r === r && selectedSquare.c === c) {
      setSelectedSquare(null);
      return;
    }

    const targetPiece = board[r][c];
    if (targetPiece && targetPiece.color === myColor) {
      setSelectedSquare({ r, c });
      return;
    }

    const movingPiece = board[selectedSquare.r][selectedSquare.c];

    // Check if player clicked the adjacent enemy pawn that just double-stepped for en passant
    let targetRow = r;
    let targetCol = c;
    if (
      movingPiece &&
      movingPiece.type === "PAWN" &&
      lastMove &&
      Math.abs(lastMove.from.r - lastMove.to.r) === 2 &&
      lastMove.to.r === selectedSquare.r &&
      Math.abs(selectedSquare.c - lastMove.to.c) === 1 &&
      r === lastMove.to.r &&
      c === lastMove.to.c
    ) {
      const forwardDir = myColor === "WHITE" ? 1 : -1;
      targetRow = selectedSquare.r + forwardDir;
      targetCol = lastMove.to.c;
    }

    const isPawnPromotion = movingPiece && movingPiece.type === "PAWN" &&
      ((movingPiece.color === "WHITE" && targetRow === 7) || (movingPiece.color === "BLACK" && targetRow === 0));

    if (isPawnPromotion) {
      setPendingPromotion({ from: selectedSquare, to: { r: targetRow, c: targetCol } });
      setSelectedSquare(null);
      return;
    }

    sendMove(selectedSquare.r, selectedSquare.c, targetRow, targetCol, null);
    setSelectedSquare(null);
  };

  const sendMove = (fromRow: number, fromCol: number, toRow: number, toCol: number, promotionPiece: string | null) => {
    if (stompClientRef.current && stompClientRef.current.connected && matchData) {
      stompClientRef.current.publish({
        destination: `/app/game/${matchData.gameId}/move`,
        body: JSON.stringify({
          fromRow,
          fromCol,
          toRow,
          toCol,
          promotionPiece
        })
      });
    }
  };

  const choosePromotion = (pieceType: "QUEEN" | "ROOK" | "BISHOP" | "KNIGHT") => {
    if (!pendingPromotion) return;
    sendMove(pendingPromotion.from.r, pendingPromotion.from.c, pendingPromotion.to.r, pendingPromotion.to.c, pieceType);
    setPendingPromotion(null);
  };

  const handleResign = () => {
    if (!matchData || !stompClientRef.current || gameOver) return;
    stompClientRef.current.publish({
      destination: `/app/game/${matchData.gameId}/resign`,
      body: JSON.stringify({ playerUsername: user?.username })
    });
  };

  const handleLeaveGame = async () => {
    if (user) {
      await fetch(`${API_BASE}/api/matchmaking/leave`, {
        method: "POST",
        headers: { Authorization: `Bearer ${user.token}` }
      }).catch(() => {});
    }
    if (stompClientRef.current) {
      stompClientRef.current.deactivate();
      stompClientRef.current = null;
    }
    setMatchData(null);
    setCheckedKingColor(null);
    setPendingPromotion(null);
    setLastMove(null);
    setMoveHistory([]);
  };

  // Auth Screen
  if (!user) {
    return (
      <div className="container">
        <div className="auth-header">
          <h1 className="auth-title">Royal Chess</h1>
          <p className="auth-subtitle">Sign in or create an account to start playing</p>
        </div>
        <div className="card">
          <div className="tabs">
            <button
              className={`tab ${authTab === "login" ? "active" : ""}`}
              onClick={() => { setAuthTab("login"); setAuthError(""); setShowPassword(false); }}
            >
              Sign In
            </button>
            <button
              className={`tab ${authTab === "register" ? "active" : ""}`}
              onClick={() => { setAuthTab("register"); setAuthError(""); setShowPassword(false); }}
            >
              Sign Up
            </button>
          </div>

          <form onSubmit={handleAuth}>
            <div className="form-group">
              <label>{authTab === "login" ? "Username or Email" : "Username"}</label>
              <input
                type="text"
                required
                value={usernameInput}
                onChange={(e) => setUsernameInput(e.target.value)}
              />
            </div>

            {authTab === "register" && (
              <div className="form-group">
                <label>Email</label>
                <input
                  type="email"
                  required
                  value={emailInput}
                  onChange={(e) => setEmailInput(e.target.value)}
                />
              </div>
            )}

            <div className="form-group">
              <label>Password</label>
              <div className="password-wrapper">
                <input
                  type={showPassword ? "text" : "password"}
                  required
                  value={passwordInput}
                  onChange={(e) => setPasswordInput(e.target.value)}
                />
                <button
                  type="button"
                  className="password-toggle"
                  onClick={() => setShowPassword(!showPassword)}
                  aria-label={showPassword ? "Hide password" : "Show password"}
                >
                  {showPassword ? (
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                      <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24" />
                      <line x1="1" y1="1" x2="23" y2="23" />
                    </svg>
                  ) : (
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                      <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                      <circle cx="12" cy="12" r="3" />
                    </svg>
                  )}
                </button>
              </div>
            </div>

            {authError && <div className="error-msg">{authError}</div>}

            <button type="submit" className="primary">
              {authTab === "login" ? "Sign In" : "Create Account"}
            </button>
          </form>
        </div>
      </div>
    );
  }

  // Lobby / Matchmaking Screen
  if (!matchData) {
    return (
      <div className="container">
        <div className="card">
          <div className="profile-bar">
            <div className="profile-info">
              <div className="profile-avatar">{user.username.slice(0, 1).toUpperCase()}</div>
              <div>
                <div className="profile-name">{user.username}</div>
                <div className="profile-elo">{user.eloRating} Elo Rating</div>
              </div>
            </div>
            <button className="secondary" onClick={handleLogout}>
              Sign Out
            </button>
          </div>

          <div className="matchmaking-center">
            {searching ? (
              <>
                <div className="search-pulse">
                  <div className="pulse-dot"></div>
                  <span>Searching for opponent...</span>
                </div>
                <button className="danger" onClick={cancelMatchmaking}>
                  Cancel Search
                </button>
              </>
            ) : (
              <button className="primary" onClick={startMatchmaking}>
                Find Match (Ranked)
              </button>
            )}
          </div>
        </div>
      </div>
    );
  }

  // Active Game Arena
  const myColor = user.username === matchData.whitePlayer ? "WHITE" : "BLACK";
  const opponentName = myColor === "WHITE" ? matchData.blackPlayer : matchData.whitePlayer;

  // Perspective row & column order
  const displayRows = myColor === "WHITE" ? [7, 6, 5, 4, 3, 2, 1, 0] : [0, 1, 2, 3, 4, 5, 6, 7];
  const displayCols = myColor === "WHITE" ? [0, 1, 2, 3, 4, 5, 6, 7] : [7, 6, 5, 4, 3, 2, 1, 0];

  // Material calculations
  const whiteScore = capturedBlack.reduce((acc, p) => acc + (PIECE_VALUES[p] || 0), 0);
  const blackScore = capturedWhite.reduce((acc, p) => acc + (PIECE_VALUES[p] || 0), 0);
  const whiteDiff = whiteScore - blackScore;
  const blackDiff = blackScore - whiteScore;

  const opponentCaptured = myColor === "WHITE" ? capturedWhite : capturedBlack;
  const playerCaptured = myColor === "WHITE" ? capturedBlack : capturedWhite;
  const opponentDiff = myColor === "WHITE" ? blackDiff : whiteDiff;
  const playerDiff = myColor === "WHITE" ? whiteDiff : blackDiff;

  // Selected piece legal move hints
  const selectedMoves = selectedSquare
    ? calculateClientLegalMoves(board, selectedSquare, lastMove)
    : [];

  return (
    <div className="game-arena">
      {/* LEFT COLUMN: Chess Board & Player Headers */}
      <div className="board-column">
        {/* Opponent Player Card (Top) */}
        <div className={`player-card ${currentTurn !== myColor && !gameOver ? "active-turn" : ""}`}>
          <div className="player-meta">
            <div className="player-icon-badge">{opponentName.slice(0, 1).toUpperCase()}</div>
            <div className="player-name-box">
              <span className="player-title">
                {opponentName}
                {currentTurn !== myColor && !gameOver && <span className="turn-pill">Thinking</span>}
              </span>
              <span className="player-color-tag">{myColor === "WHITE" ? "Black" : "White"}</span>
            </div>
          </div>
          <div className="captured-tray">
            {opponentCaptured.map((p, idx) => (
              <span key={idx} className="captured-piece-sym">
                {UNICODE_PIECES[`${myColor === "WHITE" ? "WHITE" : "BLACK"}_${p}`]}
              </span>
            ))}
            {opponentDiff > 0 && <span className="material-diff">+{opponentDiff}</span>}
          </div>
        </div>

        {/* Board Frame with In-Square Authentic Tournament Coordinates */}
        <div className="board-frame">
          <div className="chess-board">
            {displayRows.map((r, rIndex) =>
              displayCols.map((c, cIndex) => {
                const piece = board[r] ? board[r][c] : null;
                const isLight = (r + c) % 2 !== 0;
                const isSelected = selectedSquare?.r === r && selectedSquare?.c === c;
                const isLastMoveSquare =
                  (lastMove?.from.r === r && lastMove?.from.c === c) ||
                  (lastMove?.to.r === r && lastMove?.to.c === c);

                const isCheckActive = gameStatus === "CHECK" || gameStatus === "CHECKMATE";
                const activeCheckedColor = checkedKingColor || currentTurn;
                const isKingInCheck = piece && piece.type === "KING" && isCheckActive && piece.color === activeCheckedColor;
                const pieceChar = piece ? UNICODE_PIECES[`${piece.color}_${piece.type}`] : "";
                const legalMove = selectedMoves.find(m => m.r === r && m.c === c);

                // Coordinates:
                // Rank numbers 1-8 shown on the leftmost column from player's view
                const showRankCoord = cIndex === 0;
                const rankNumber = r + 1;

                // File letters a-h shown on the bottom row from player's view
                const showFileCoord = rIndex === 7;
                const fileLetter = String.fromCharCode(97 + c);

                return (
                  <div
                    key={`${r}-${c}`}
                    className={`square ${isLight ? "light" : "dark"} ${isSelected ? "selected" : ""} ${isLastMoveSquare ? "last-move" : ""} ${isKingInCheck ? "check-king" : ""}`}
                    onClick={() => handleSquareClick(r, c)}
                    title={`${fileLetter}${rankNumber}`}
                  >
                    {/* Rank Number (1-8) in top-left */}
                    {showRankCoord && <span className="coord-rank">{rankNumber}</span>}

                    {/* File Letter (a-h) in bottom-right */}
                    {showFileCoord && <span className="coord-file">{fileLetter}</span>}

                    {isKingInCheck && <span className="check-sign-badge">!</span>}

                    {/* Legal Move Hint */}
                    {legalMove && (
                      legalMove.isCapture ? (
                        <span className="capture-hint-ring" />
                      ) : (
                        <span className="move-hint-dot" />
                      )
                    )}

                    {piece && (
                      <span className={piece.color === "WHITE" ? "piece-white" : "piece-black"}>
                        {pieceChar}
                      </span>
                    )}
                  </div>
                );
              })
            )}
          </div>
        </div>

        {/* Current User Player Card (Bottom) */}
        <div className={`player-card ${currentTurn === myColor && !gameOver ? "active-turn" : ""}`}>
          <div className="player-meta">
            <div className="player-icon-badge">{user.username.slice(0, 1).toUpperCase()}</div>
            <div className="player-name-box">
              <span className="player-title">
                {user.username}
                {currentTurn === myColor && !gameOver && <span className="turn-pill">Your Turn</span>}
              </span>
              <span className="player-color-tag">{myColor === "WHITE" ? "White" : "Black"} ({user.eloRating} Elo)</span>
            </div>
          </div>
          <div className="captured-tray">
            {playerCaptured.map((p, idx) => (
              <span key={idx} className="captured-piece-sym">
                {UNICODE_PIECES[`${myColor === "WHITE" ? "BLACK" : "WHITE"}_${p}`]}
              </span>
            ))}
            {playerDiff > 0 && <span className="material-diff">+{playerDiff}</span>}
          </div>
        </div>
      </div>

      {/* RIGHT COLUMN: Move Notation Log & Control Panel */}
      <div className="side-panel">
        <div className="panel-header">
          <span className="panel-title">Match Dashboard</span>
          <span className={`status-badge ${gameStatus === "CHECK" ? "check" : "active"}`}>
            {gameStatus === "CHECK" ? "CHECK" : gameStatus === "CHECKMATE" ? "CHECKMATE" : `${currentTurn} to move`}
          </span>
        </div>

        {/* Live Notation Table */}
        <div className="notation-box" ref={notationBoxRef}>
          <div className="notation-header">
            <span>#</span>
            <span>White</span>
            <span>Black</span>
          </div>
          {moveHistory.length === 0 ? (
            <div className="notation-empty">Moves will appear here as played</div>
          ) : (
            moveHistory.map((item) => (
              <div key={item.moveNumber} className="notation-row">
                <span className="notation-num">{item.moveNumber}.</span>
                <span className="notation-move">{item.white}</span>
                <span className="notation-move">{item.black || ""}</span>
              </div>
            ))
          )}
        </div>

        {/* Match Action Controls */}
        <div className="panel-actions">
          {!gameOver && (
            <button className="danger" onClick={handleResign}>
              Resign Game
            </button>
          )}

          {gameOver && (
            <div className="game-over-banner">
              <div className="game-over-title">Game Over: {gameStatus}</div>
              {winner && <div className="game-over-winner">Winner: {winner}</div>}
              <div>Updated Elo: {user.eloRating}</div>
              <button className="primary" onClick={handleLeaveGame}>
                Return to Lobby
              </button>
            </div>
          )}
        </div>
      </div>

      {/* Pawn Promotion Modal Overlay */}
      {pendingPromotion && (
        <div className="promotion-overlay">
          <div className="promotion-modal">
            <div className="promotion-title">Select Pawn Promotion</div>
            <div className="promotion-pieces">
              {(["QUEEN", "ROOK", "BISHOP", "KNIGHT"] as const).map((p) => (
                <button
                  key={p}
                  className="promotion-btn"
                  onClick={() => choosePromotion(p)}
                  title={`Promote to ${p}`}
                >
                  <span className={myColor === "WHITE" ? "piece-white" : "piece-black"}>
                    {UNICODE_PIECES[`${myColor}_${p}`]}
                  </span>
                </button>
              ))}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
