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
  check: boolean;
  checkedKingColor?: string;
  gameOver: boolean;
  winner?: string;
  whiteElo?: number;
  blackElo?: number;
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

const API_BASE = "http://localhost:8080";
const WS_BASE = "ws://localhost:8080/ws";

function playSound(type: "move" | "capture" | "check" | "gameover") {
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
      osc.frequency.setValueAtTime(350, ctx.currentTime);
      osc.frequency.exponentialRampToValueAtTime(150, ctx.currentTime + 0.08);
      gain.gain.setValueAtTime(0.3, ctx.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.01, ctx.currentTime + 0.08);
      osc.start();
      osc.stop(ctx.currentTime + 0.08);
    } else if (type === "capture") {
      osc.type = "triangle";
      osc.frequency.setValueAtTime(550, ctx.currentTime);
      osc.frequency.exponentialRampToValueAtTime(120, ctx.currentTime + 0.12);
      gain.gain.setValueAtTime(0.4, ctx.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.01, ctx.currentTime + 0.12);
      osc.start();
      osc.stop(ctx.currentTime + 0.12);
    } else if (type === "check") {
      osc.type = "sawtooth";
      osc.frequency.setValueAtTime(500, ctx.currentTime);
      osc.frequency.setValueAtTime(650, ctx.currentTime + 0.08);
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
  const [currentTurn, setCurrentTurn] = useState<"WHITE" | "BLACK">("WHITE");
  const [gameStatus, setGameStatus] = useState("ACTIVE");
  const [checkedKingColor, setCheckedKingColor] = useState<string | null>(null);
  const [gameOver, setGameOver] = useState(false);
  const [winner, setWinner] = useState<string | null>(null);
  const [pendingPromotion, setPendingPromotion] = useState<{ from: { r: number; c: number }; to: { r: number; c: number } } | null>(null);

  const stompClientRef = useRef<Client | null>(null);
  const pollIntervalRef = useRef<NodeJS.Timeout | null>(null);

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
          setBoard(prev => {
            const next = prev.map(row => [...row]);
            const piece = next[state.fromRow][state.fromCol];
            next[state.fromRow][state.fromCol] = null;
            if (state.promotionPiece) {
              const promoType = state.promotionPiece as "QUEEN" | "ROOK" | "BISHOP" | "KNIGHT";
              next[state.toRow][state.toCol] = { type: promoType, color: piece ? piece.color : "WHITE" };
            } else {
              next[state.toRow][state.toCol] = piece;
            }
            return next;
          });

          setCurrentTurn(state.currentTurn);
          setGameStatus(state.status);
          setCheckedKingColor(state.check ? (state.checkedKingColor || state.currentTurn) : null);

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
          } else if (state.capturedPiece) {
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
    const isPawnPromotion = movingPiece && movingPiece.type === "PAWN" &&
      ((movingPiece.color === "WHITE" && r === 7) || (movingPiece.color === "BLACK" && r === 0));

    if (isPawnPromotion) {
      setPendingPromotion({ from: selectedSquare, to: { r, c } });
      setSelectedSquare(null);
      return;
    }

    sendMove(selectedSquare.r, selectedSquare.c, r, c, null);
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
  };

  if (!user) {
    return (
      <div className="container">
        <div className="auth-header">
          <h1 className="auth-title">Welcome</h1>
          <p className="auth-subtitle">Sign in or create an account to play</p>
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

  if (!matchData) {
    return (
      <div className="container">
        <div className="card">
          <div className="profile-bar">
            <div className="profile-info">
              <div className="profile-name">{user.username}</div>
              <div className="profile-elo">{user.eloRating} Elo</div>
            </div>
            <button className="secondary" onClick={handleLogout}>
              Sign Out
            </button>
          </div>

          <div className="matchmaking-center">
            {searching ? (
              <>
                <div className="status-badge">Searching for opponent...</div>
                <button className="danger" onClick={cancelMatchmaking}>
                  Cancel
                </button>
              </>
            ) : (
              <button className="primary" onClick={startMatchmaking}>
                Find Match
              </button>
            )}
          </div>
        </div>
      </div>
    );
  }

  const myColor = user.username === matchData.whitePlayer ? "WHITE" : "BLACK";
  const opponentName = myColor === "WHITE" ? matchData.blackPlayer : matchData.whitePlayer;

  const displayRows = myColor === "WHITE" ? [7, 6, 5, 4, 3, 2, 1, 0] : [0, 1, 2, 3, 4, 5, 6, 7];
  const displayCols = myColor === "WHITE" ? [0, 1, 2, 3, 4, 5, 6, 7] : [7, 6, 5, 4, 3, 2, 1, 0];

  return (
    <div className="board-container">
      <div className="game-header">
        <div>
          Opponent: <span className="player-tag">{opponentName}</span> ({myColor === "WHITE" ? "Black" : "White"})
        </div>
        <div className="status-badge">
          {gameStatus === "CHECK" ? "CHECK!" : `${currentTurn === "WHITE" ? "White" : "Black"} to move`}
        </div>
      </div>

      <div className="chess-board">
        {displayRows.map((r) =>
          displayCols.map((c) => {
            const piece = board[r] ? board[r][c] : null;
            const isLight = (r + c) % 2 !== 0;
            const isSelected = selectedSquare?.r === r && selectedSquare?.c === c;
            const isCheckActive = gameStatus === "CHECK" || gameStatus === "CHECKMATE";
            const activeCheckedColor = checkedKingColor || currentTurn;
            const isKingInCheck = piece && piece.type === "KING" && isCheckActive && piece.color === activeCheckedColor;
            const pieceChar = piece ? UNICODE_PIECES[`${piece.color}_${piece.type}`] : "";

            return (
              <div
                key={`${r}-${c}`}
                className={`square ${isLight ? "light" : "dark"} ${isSelected ? "selected" : ""} ${isKingInCheck ? "check-king" : ""}`}
                onClick={() => handleSquareClick(r, c)}
              >
                {isKingInCheck && <span className="check-sign-badge">!</span>}
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

      <div className="game-controls">
        <div>
          You: <span className="player-tag">{user.username}</span> ({myColor === "WHITE" ? "White" : "Black"})
        </div>
        {!gameOver && (
          <button className="danger" onClick={handleResign}>
            Resign
          </button>
        )}
      </div>

      {pendingPromotion && (
        <div className="promotion-overlay">
          <div className="promotion-modal">
            <div className="promotion-title">Promote Pawn</div>
            <div className="promotion-pieces">
              {(["QUEEN", "ROOK", "BISHOP", "KNIGHT"] as const).map((p) => (
                <button
                  key={p}
                  className="promotion-btn"
                  onClick={() => choosePromotion(p)}
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

      {gameOver && (
        <div className="game-over-banner">
          <div>
            <strong>Game Over: {gameStatus}</strong>
          </div>
          {winner && <div>Winner: {winner}</div>}
          <div>Updated Elo: {user.eloRating}</div>
          <button className="primary" onClick={handleLeaveGame}>
            Return to Home
          </button>
        </div>
      )}
    </div>
  );
}
