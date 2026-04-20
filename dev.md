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
