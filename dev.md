# Dev Notes

## 2026-04-20 1:20 PM

### Cleanup Direction Confirmed
- Keep only opponent-to-opponent chat.
- Remove group chat, broadcast, and view-users features.
- Keep server headless for now, but do not fully remove `GuiServer.java`; strip it down.
- Trim `Message.java` to only fields/types needed for join + direct messaging.

### Changes Completed
- Reworked client GUI to:
	- Join with unique username first.
	- Send direct messages only (requires opponent username).
	- Remove chat-assignment extras (group/broadcast-related UI).
- Reworked server to:
	- Accept unique username joins.
	- Route direct messages only (`SEND_ONE`).
	- Return basic errors for missing receiver / receiver not found.
	- Remove old group and broadcast paths.
- Replaced heavy server GUI with a minimal stripped version that just starts the server.
- Trimmed `Message.java` in both client and server projects to only:
	- `JOIN`
	- `SEND_ONE`
	- core fields (`statusCode`, `messageType`, `userID`, `receiverID`, `messageBody`)

### Validation
- Compiled both Maven modules successfully after cleanup.
- Note: `target` build outputs were updated by compilation (generated artifacts).

## 2026-04-20 1:40 PM

### Checkers Slice Implemented (Playable Skeleton)

1. File: HW5Client/src/main/java/Message.java
- Expanded protocol from chat-only to checkers session/game messaging.
- Added message types: JOIN, WAITING, MATCH_FOUND, CHAT, MOVE, BOARD_STATE, GAME_OVER, ERROR.
- Added session/game fields: gameID, playerColor, opponentID, boardState, turnColor.
- Added move coordinate fields: fromRow, fromCol, toRow, toCol.
- Kept common transport fields: statusCode, messageType, userID, receiverID, messageBody.

2. File: HW5Server/src/main/java/Message.java
- Matched client Message structure exactly so serialization remains compatible.
- Added same message types and gameplay/session fields as client Message.
- Added same move coordinate fields and getters/setters.

3. File: HW5Server/src/main/java/Server.java
- Replaced direct-chat-only server flow with game-session server flow.
- Added auto-pair queue logic to match two joined users into one game session.
- Added GameSession model on server with: gameID, redPlayer, blackPlayer, board, turnColor, active.
- Added initial checkers board generation (red top rows, black bottom rows, dark squares only).
- Added server-authoritative move validation for first playable pass:
	- diagonal moves
	- single captures
	- king promotion
	- turn enforcement (red starts every game)
- Added board serialization and BOARD_STATE broadcast to both players after valid moves.
- Added game-end checks and GAME_OVER messages:
	- all opponent pieces captured
	- opponent has no legal moves
	- opponent disconnects
- Added match-only CHAT routing between paired opponents in active game only.
- Kept duplicate username rejection on join.

4. File: HW5Client/src/main/java/GuiClient.java
- Replaced text-only chat UI with checkers client layout.
- Added join controls at top (username + join button + status label).
- Added 8x8 clickable board grid in center.
- Added bottom chat panel (chat list + input + send button).
- Added message handling for JOIN, WAITING, MATCH_FOUND, BOARD_STATE, CHAT, GAME_OVER, ERROR.
- Added local board rendering from server boardState string.
- Added move-click flow:
	- click own piece to select
	- click destination to send MOVE message
	- clear selection after sending
- Enforced client-side guardrails for UX:
	- must be in active game to move/chat
	- must be player's turn to send move
- Kept fixed board orientation for both players.

### Validation
- Compiled successfully:
	- HW5Client (`mvn -q -DskipTests compile`)
	- HW5Server (`mvn -q -DskipTests compile`)
- Committed on `dev` in 2 commits:
	- 545906d (Message protocol expansion)
	- b2f047f (Server game session logic + client board/chat UI)

## 2026-04-20 2:XX PM

### Rules + Requirement Completion Updates

1. File: HW5Server/src/main/java/Server.java
- Added mandatory capture enforcement (normal move blocked when any capture exists).
- Added multi-jump continuation enforcement:
	- same player must continue jump with same piece when another capture is available.
	- turn does not switch until jump chain ends.
- Added CSGO-style server log tags across major events:
	- `[SERVER]`, `[CONNECT]`, `[QUEUE]`, `[MATCH START]`, `[MOVE]`, `[CHAT]`, `[REJECT]`, `[ROUND END]`, `[DISCONNECT]`, `[ERROR]`.
- Added draw logic required by rubric:
	- draw when neither side has any legal move.
- Added end-of-game decision handling for required flow:
	- rematch request handling (both players must request `Play Again`).
	- quit handling (`Quit`) with session cleanup and opponent notification.
- Added game/session cleanup helper to prevent stale sessions.

2. File: HW5Client/src/main/java/GuiClient.java
- Added end-of-game controls:
	- `Play Again` button.
	- `Quit` button.
- Enabled buttons after `GAME_OVER`, disabled during active game.
- Wired `Play Again` to send `REMATCH` message.
- Wired `Quit` to send `QUIT` message and close local game flow.

3. File: HW5Client/src/main/java/Message.java
- Added message types:
	- `REMATCH`
	- `QUIT`

4. File: HW5Server/src/main/java/Message.java
- Added message types:
	- `REMATCH`
	- `QUIT`

5. File: HW5Server/src/main/java/GuiServer.java
- Replaced minimal placeholder with active server log window.
- Server callback now appends live logs to GUI `ListView` and prints to console.

### Validation
- Compiled successfully after updates:
	- HW5Client (`mvn -q -DskipTests compile`)
	- HW5Server (`mvn -q -DskipTests compile`)
- Additional commits on `dev`:
	- 6ee9730 (mandatory capture + multi-jump + CSGO logs)
	- 6369896 (draw + play-again/quit + visible server logs)

## 2026-04-20 2:22 PM - Image Cleanup

### File: HW5Client/src/main/java/GuiClient.java
- Simplified the piece image code so it looks more beginner-friendly.
- Removed the separate helper methods for loading images and building image graphics.
- Loaded the four piece images directly in the UI setup code.
- Kept the same piece rendering behavior on the board.
- Kept fallback text rendering if an image is not available.

### File: HW5Client/src/main/resources/checkers_images/red.png
- Added red piece image asset for board rendering.

### File: HW5Client/src/main/resources/checkers_images/red_king.png
- Added red king image asset for board rendering.

### File: HW5Client/src/main/resources/checkers_images/black.png
- Added black piece image asset for board rendering.

### File: HW5Client/src/main/resources/checkers_images/black_king.png
- Added black king image asset for board rendering.

### Validation
- Compiled successfully after the image cleanup.
- Commit on `dev`:
	- 2520e2c (Simplify piece image rendering in client GUI)

