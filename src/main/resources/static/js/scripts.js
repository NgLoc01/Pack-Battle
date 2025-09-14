// Connect to the WebSocket endpoint
const socket = new SockJS('/pack-websocket');
const stompClient = Stomp.over(socket);

let stompSessionId = null;
const rooms = ["favoritesRoom", "universesBeyondRoom"];

stompClient.connect({}, function(frame) { 
    console.log('Connected: ' + frame);
    
    // Get session ID from STOMP frame headers - more reliable method
    stompSessionId = frame.headers['session'];
    console.log("Session ID from frame:", stompSessionId);
    
    if (stompSessionId) {
        sessionStorage.setItem("sessionId", stompSessionId);
        console.log('✅ Session ID stored:', stompSessionId);
        
        // Set up subscriptions immediately since we have the session ID
        setupOtherSubscriptions();
    } else {
        console.error("Failed to get session ID from STOMP frame");
        // Fallback: wait for server to send session ID
        waitForServerSessionId();
    }
    
}, function(error) {
    console.error('STOMP connection error:', error);
    alert('Connection failed. Please refresh the page.');
});

function waitForServerSessionId() {
    // Fallback method - wait for server confirmation
    console.log("Waiting for server to send session ID...");
    
    // Create a temporary subscription to catch any session ID broadcast
    const tempSub = stompClient.subscribe('/topic/session-broadcast', function(message) {
        const receivedSessionId = message.body;
        console.log('✅ Server sent session ID:', receivedSessionId);
        sessionStorage.setItem("sessionId", receivedSessionId);
        stompSessionId = receivedSessionId;
        
        // Clean up temp subscription
        tempSub.unsubscribe();
        
        // Now set up other subscriptions
        setupOtherSubscriptions();
    });
    
    // Also try the original method as additional fallback
    setTimeout(() => {
        if (!stompSessionId) {
            console.log("Trying original session ID extraction method...");
            let rawSessionId = null;
            try {
                if (socket._transport && socket._transport.url) {
                    const urlParts = socket._transport.url.split('/');
                    rawSessionId = urlParts[urlParts.length - 2];
                    console.log("Extracted session ID from URL:", rawSessionId);
                }
            } catch (error) {
                console.error("Could not extract session ID from URL:", error);
            }
            
            if (rawSessionId && !stompSessionId) {
                stompSessionId = rawSessionId;
                sessionStorage.setItem("sessionId", rawSessionId);
                setupOtherSubscriptions();
            }
        }
    }, 1000);
}

function setupOtherSubscriptions() {
    console.log("Setting up subscriptions with session ID:", stompSessionId);
    
    //Subscribe to user count updates for each room
    rooms.forEach(roomName => {
        stompClient.subscribe(`/topic/${roomName}/count`, function(message) {
            const count = JSON.parse(message.body);
            updateUserCountDisplay(roomName, count);
        });
    });

    stompClient.subscribe(`/topic/rooms/pack-picks`, function(message) {
        const pickedPack = message.body;
        console.log("📦 Opponent picked:", pickedPack);
        if (typeof disablePackButton !== 'undefined') {
            disablePackButton(pickedPack);
        }
    });

    stompClient.subscribe(`/topic/rooms/pack-unpicks`, function (message) {
        const packName = message.body;
        if (typeof enablePackButton !== 'undefined') {
            enablePackButton(packName);
        }
    });

    // Subscribe to money updates for current room only
    const currentRoom = getCurrentRoomName();
    if (currentRoom) {
        stompClient.subscribe(`/topic/${currentRoom}/money`, function(message) {
            const update = JSON.parse(message.body);

            const myId = sessionStorage.getItem("sessionId")?.trim();
            const isYou = myId && update.sessionId && update.sessionId === myId;

            const youElement = document.querySelector(".money-you");
            const opponentElement = document.querySelector(".money-opponent");
            
            if (youElement) {
                youElement.textContent = `You: $${isYou ? update.yourMoney : update.opponentMoney}`;
            }
            if (opponentElement) {
                opponentElement.textContent = `Opponent: $${isYou ? update.opponentMoney : update.yourMoney}`;
            }
        });
    }
}

// Get the current room name from the URL path
function getCurrentRoomName() {
    const path = window.location.pathname;
    const match = path.match(/\/room\/([^\/]+)/);
    return match ? match[1] : null;
}

// Fetch current user counts on page load
document.addEventListener("DOMContentLoaded", () => {
    rooms.forEach(fetchUserCount);
});

function fetchUserCount(roomName) {
    fetch(`/api/room/${roomName}/count`)
        .then(res => res.json())
        .then(count => updateUserCountDisplay(roomName, count))
        .catch(error => console.error('Error fetching user count:', error));
}

function updateUserCountDisplay(roomName, count) {
    const display = document.getElementById(`${roomName}-user-count`);
    const roomCard = document.getElementById(`clickable-roomCard-${roomName}`);

    if (!display) return;
    display.textContent = `Users connected: ${count} / 2`;

    if (roomCard) {
        roomCard.style.pointerEvents = count >= 2 ? "none" : "auto";
        roomCard.style.opacity = count >= 2 ? 0.5 : 1;
    }
}

function joinRoom(roomName) {
    const sessionId = sessionStorage.getItem("sessionId")?.trim();
    
    if (!sessionId) {
        console.error("No session ID available");
        console.log("Session storage contents:", sessionStorage.getItem("sessionId"));
        console.log("STOMP connected:", stompClient?.connected);
        console.log("STOMP session ID:", stompSessionId);
        
        alert("Connection not ready. Please wait a moment and try again.");
        return;
    }

    console.log("Attempting to join room with session ID:", sessionId);

    fetch(`/room/${roomName}/join?sessionId=${encodeURIComponent(sessionId)}`, {
        method: 'POST'
    }).then(res => {
        if (res.ok) {
            sessionStorage.setItem("joinedSessionId", sessionId);
            window.location.href = `/room/${roomName}`;
        } else {
            return res.text().then(text => {
                throw new Error(text || "Room is full");
            });
        }
    }).catch(error => {
        console.error('Error joining room:', error);
        alert(error.message || "Failed to join room. Please try again.");
    });
}

function leaveRoom(roomName) {
    const sessionId = sessionStorage.getItem("joinedSessionId")?.trim();
    if (!sessionId) {
        alert("You haven't joined this room in this session.");
        return;
    }

    fetch(`/room/${roomName}/leave?sessionId=${encodeURIComponent(sessionId)}`, {
        method: 'POST'
    }).then(res => {
        if (res.ok) {
            sessionStorage.removeItem("joinedSessionId");
            sessionStorage.removeItem("sessionId");
            window.location.href = "/";
        } else {
            alert("Failed to leave the room.");
        }
    }).catch(error => {
        console.error('Error leaving room:', error);
        alert("Failed to leave room.");
    });
}