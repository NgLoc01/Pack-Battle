// Connect to the WebSocket endpoint
const socket = new SockJS('/pack-websocket');
const stompClient = Stomp.over(socket);

let stompSessionId = null;
const rooms = ["favoritesRoom", "universesBeyondRoom"];


stompClient.connect({}, function(frame) { 
    console.log('Connected: ' + frame);

    const rawSessionId = socket._transport.url.split('/').slice(-2, -1)[0]; // Extract the raw session ID from the SockJS URL
    console.log("WebSocket raw sessionId (SockJS URL):", rawSessionId);

    //Subscribe to the session topic to receive the session ID from the server (WebSocketEventListener.java)
    stompClient.subscribe(`/topic/session/${rawSessionId}`, function(message) { 
        const receivedSessionId = message.body;
        console.log('✅ Received session ID from server:', receivedSessionId);
        sessionStorage.setItem("sessionId", receivedSessionId);
        stompSessionId = receivedSessionId;
    });

    //Subscribe to user count updates for each room
    rooms.forEach(roomName => {
        stompClient.subscribe(`/topic/${roomName}/count`, function(message) {
            const count = JSON.parse(message.body);
            updateUserCountDisplay(roomName, count);
        });
    });

    
    stompClient.subscribe(`/topic/rooms/pack-picks`, function(message) {
        const pickedPack = message.body;
        console.log("🔄 Opponent picked:", pickedPack);
        disablePackButton(pickedPack); // disables the pack on both sides
    });

    stompClient.subscribe(`/topic/rooms/pack-unpicks`, function (message) {
      const packName = message.body;
      enablePackButton(packName);
    });

    // Subscribe to money updates for current room only
    const currentRoom = getCurrentRoomName();
    if (currentRoom) {
        stompClient.subscribe(`/topic/${currentRoom}/money`, function(message) {
            const update = JSON.parse(message.body);

            // Determine who is "you" based on sessionId
            const myId = sessionStorage.getItem("sessionId")?.trim();
            const isYou = myId && update.sessionId && update.sessionId === myId;

            document.querySelector(".money-you").textContent = `You: $${isYou ? update.yourMoney : update.opponentMoney}`;
            document.querySelector(".money-opponent").textContent = `Opponent: $${isYou ? update.opponentMoney : update.yourMoney}`;
        });
    }

});

// Get the current room name from the URL path
function getCurrentRoomName() {
    const path = window.location.pathname;
    const match = path.match(/\/room\/([^\/]+)/);
    return match ? match[1] : null;
}

// Fetch current user counts on page load
document.addEventListener("DOMContentLoaded", () => { // Ensure the content is fully loaded before fetching counts
    rooms.forEach(fetchUserCount);
});

function fetchUserCount(roomName) { // Fetch the user count for a specific room
    fetch(`/api/room/${roomName}/count`) //fetches from getRoomUserCount in RoomController.java
        .then(res => res.json())
        .then(count => updateUserCountDisplay(roomName, count));
}


function updateUserCountDisplay(roomName, count) {  // Update the user count display and room card state
    const display = document.getElementById(`${roomName}-user-count`);

    const roomCard = document.getElementById(`clickable-roomCard-${roomName}`); // User count from index   
    const leftColumn = document.getElementById("left-column");                  // User count from other rooms

    if (!display) return;
    display.textContent = `Users connected: ${count} / 2`; //always try too display users

    if (roomCard) { //Only for index page, make unclickable if count >= 2
        roomCard.style.pointerEvents = count >= 2 ? "none" : "auto";
        roomCard.style.opacity = count >= 2 ? 0.5 : 1;
    } else if (leftColumn) { //Do nothing for other pages
        // Do nothing – don't touch left-column!
    }
}


function joinRoom(roomName) { //call for joinRoom comes from html with room parameter and continues to joinRoom in RoomController.java
    const sessionId = sessionStorage.getItem("sessionId")?.trim();
    if (!sessionId) {
        alert("No session ID yet. Please wait.");
        return;
    }

    fetch(`/room/${roomName}/join?sessionId=${encodeURIComponent(sessionId)}`, { //fetches from joinRoom in RoomController.java
        method: 'POST'
    }).then(res => {
        if (res.ok) {
            sessionStorage.setItem("joinedSessionId", sessionId);
            window.location.href = `/room/${roomName}`;
        } else {
            alert("Room is full.");
        }
    });
}

function leaveRoom(roomName) { //call for leaveRoom comes from html with room parameter and continues to leaveRoom in RoomController.java
    const sessionId = sessionStorage.getItem("joinedSessionId")?.trim();
    if (!sessionId) {
        alert("You haven't joined this room in this session.");
        return;
    }

    fetch(`/room/${roomName}/leave?sessionId=${encodeURIComponent(sessionId)}`, { //fetches from leaveRoom in RoomController.java
        method: 'POST'
    }).then(res => {
        if (res.ok) {
            sessionStorage.removeItem("joinedSessionId");
            sessionStorage.removeItem("sessionId");
            window.location.href = "/";
        } else {
            alert("Failed to leave the room.");
        }
    });
}

