let selectedPacks = [];
let pendingPack = null;
let pendingTargetBox = null;

// Track your current money locally
let currentPlayerMoney = 0;

// Track which packs have been opened and their content
let openedPacks = new Map(); // packName -> {boxId: string, content: string}

const imageMap = {
    assasinscreed: "/images/assasinscreed.png",
    drwho: "/images/drwho.png",
    lotr: "/images/lotr.png",
    finalfantasy: "/images/finalfantasy.png",

    mh3: "/images/MH3.png",
    foundations: "/images/foundations.webp",
    duskmourn: "/images/duskmourn.png",
    bloomburrow: "/images/bloomburrow.png"
};

// Get the current room name from the URL path
function getCurrentRoomName() {
    const path = window.location.pathname;
    const match = path.match(/\/room\/([^\/]+)/);
    return match ? match[1] : null;
}

// *************** Actions ***************
function disablePackButton(packName) {
    const button = document.querySelector(`.pack-button[data-pack="${packName}"]`);
    if (button) button.disabled = true;
}

function enablePackButton(packName) {
    const button = document.querySelector(`.pack-button[data-pack="${packName}"]`);
    if (button) button.disabled = false;
}

function removeOrOpenPack(packName, targetBoxId) { //displays modal
    pendingPack = packName;
    pendingTargetBox = targetBoxId;
    document.getElementById("modal-pack-name").textContent = `What do you want to do with the pack?`;
    document.getElementById("packActionModal").style.display = "flex";
}

function closeModal() { //gets modal from html in favoritesRoom.html and universesBeyondRoom.html
    document.getElementById("packActionModal").style.display = "none";
    pendingPack = null;
    pendingTargetBox = null;
}

function handlePackClick(packName) { // goes to html onclick
    if (selectedPacks.length >= 2) {
        alert("You can only choose 2 packs!");
        return;
    }

    selectedPacks.push(packName);
    console.log(`handlePackClick, Selected packs: ${selectedPacks}`);

    const targetBoxId = selectedPacks.length === 1
    ? "selected-pack-box-1"
    : "selected-pack-box-2";

    const targetBox = document.getElementById(targetBoxId);
    // When a user clicks a pack, this displays the pack image in one of the selection boxes
    targetBox.innerHTML = ` 
    <div onclick="removeOrOpenPack('${packName}', '${targetBoxId}')"
            style="width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; cursor: pointer;">
        <img class="selected-pack" src="${imageMap[packName]}" alt="${packName}" style="max-height: 100%; max-width: 100%;" />
    </div>`;

    stompClient.send(`/app/rooms/pick-pack`, {}, packName);
}

async function confirmPackAction(action) { // goes to html onclick
    console.log(`confirmPackAction, Selected pack: ${selectedPacks}`);
    if (!pendingPack || !pendingTargetBox) return;

    if (action === 'remove') {
        // Remove the pack from selectedPacks array
        selectedPacks = selectedPacks.filter(p => p !== pendingPack);
        enablePackButton(pendingPack);

        // Remove from opened packs if it was opened
        openedPacks.delete(pendingPack);

        // Clear the specific box that contained the removed pack
        document.getElementById(pendingTargetBox).innerHTML = "<p>Select a pack to see it here</p>";

        // Re-arrange remaining packs to fill gaps, preserving their opened state
        document.getElementById("selected-pack-box-1").innerHTML = "<p>Select a pack to see it here</p>";
        document.getElementById("selected-pack-box-2").innerHTML = "<p>Select a pack to see it here</p>";

        selectedPacks.forEach((pack, index) => {
            const boxId = index === 0 ? "selected-pack-box-1" : "selected-pack-box-2";
            const box = document.getElementById(boxId);
            
            // Check if this pack was previously opened
            if (openedPacks.has(pack)) {
                // Restore the opened content
                const openedContent = openedPacks.get(pack);
                box.innerHTML = openedContent.content;
                // Update the boxId in our tracking
                openedPacks.set(pack, { ...openedContent, boxId: boxId });
            } else {
                // Re-render the unopened pack
                box.innerHTML = `
                <div onclick="removeOrOpenPack('${pack}', '${boxId}')"
                        style="width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; cursor: pointer;">
                    <img class="selected-pack" src="${imageMap[pack]}" alt="${pack}" style="max-height: 100%; max-width: 100%;" />
                </div>`;
            }
        });

        if (typeof stompClient !== 'undefined' && stompClient) {
            stompClient.send(`/app/rooms/unpick-pack`, {}, pendingPack);
        }

        closeModal();
        return;
    }

    if (action === 'open') {
        const box = document.getElementById(pendingTargetBox);

        if (box) {
            // Show loading state
            box.innerHTML = `<div class="cards-scroll">
            <p style="opacity:.7;margin:0;">Opening pack…</p>
            </div>`;
        }

        try {
            const cards = await fetchCards(12, pendingPack);
            renderCardsStripInBox(pendingTargetBox, cards);

            // Store the opened pack content for preservation during removals
            const openedContent = box.innerHTML;
            openedPacks.set(pendingPack, { 
                boxId: pendingTargetBox, 
                content: openedContent 
            });

            // Calculate total value of cards in the pack
            let packValue = 0;
            cards.forEach(card => {
                const cardPrice = parseFloat(card.prices?.usd) || 0;
                packValue += cardPrice;
                console.log(`Card: ${card.name}, Price: $${cardPrice}`);
            });

            console.log(`Total pack value: $${packValue.toFixed(2)}`);

            const roundedPackValue = Math.round(packValue * 100) / 100;

            // ADD to your current money
            currentPlayerMoney += roundedPackValue;

            // Get opponent's current money and preserve it
            const currentOpponentMoney = getCurrentOpponentMoney();

            // Send YOUR updated money but keep opponent's money unchanged
            updateMoney(currentPlayerMoney, currentOpponentMoney);

        } catch (e) {
            console.error(e);
            if (box) {
                // Show error state
                box.innerHTML = `<div class="cards-scroll">
                    <p style="margin:0;">Failed to open pack.</p>
                </div>`;
            }
        }
        closeModal();
        return;
    }

    closeModal();
}


// *************** MONEY ***************
function updateMoney(yourMoney, opponentMoney) {
    const sid = sessionStorage.getItem("sessionId")?.trim();
    const currentRoom = getCurrentRoomName();
    
    if (!sid || !currentRoom) { 
        console.warn("No sessionId or room name; skipping money update."); 
        return; 
    }
    
    const payload = { sessionId: sid, yourMoney, opponentMoney };
    stompClient.send(
        `/app/${currentRoom}/update-money`,
        { "content-type": "application/json" },
        JSON.stringify(payload)
    );
}

// Function to get current opponent money from UI
function getCurrentOpponentMoney() {
    const opponentMoneyElement = document.querySelector('.money-opponent');
    if (!opponentMoneyElement) return 0;

    const opponentText = opponentMoneyElement.textContent;
    const opponentMoney = parseFloat(opponentText.replace(/[^\d.-]/g, '')) || 0;
    return opponentMoney;
}


// *************** Scryfall ***************
async function fetchOneCard(packName) { //goes to fetchCards()
    if (packName == "drwho") packName = "who";
    else if (packName == "assasinscreed") packName = "acr";
    else if (packName == "lotr") packName = "lotr";
    else if (packName == "finalfantasy") packName = "fin";

    else if (packName == "mh3") packName = "mh3";
    else if (packName == "foundations") packName = "fdn";
    else if (packName == "duskmourn") packName = "dsk";
    else if (packName == "bloomburrow") packName = "blb";

    const url = `/api/card?mtgSet=${packName}`;
    const res = await fetch(url);
    if (!res.ok) throw new Error('Failed to fetch card');

    const card = await res.json();
    return card;
}

async function fetchCards(count, packName) { //goes to confirmPackAction()
    const requests = Array.from({ length: count }, () => fetchOneCard(packName));
    return Promise.all(requests);
}

function getCardImgUrl(card, preferredSize = 'normal') {
    if (card && card.image_uris) { // Check if card and its image_uris exist
        return card.image_uris[preferredSize] || card.image_uris.small || '';
    }

    const face = card && card.card_faces && card.card_faces[0]; // Handle double-faced cards
    if (face && face.image_uris) { // Check if face and its image_uris exist
        return face.image_uris[preferredSize] || face.image_uris.small || '';
    }

    return '';
}

function renderCardsStripInBox(boxId, cards) {
    const box = document.getElementById(boxId);
    if (!box) return;

    const imgs = cards.map(c => {
    const src = getCardImgUrl(c);
    const name = (c.name || 'Card').replace(/"/g, '&quot;');
    return src ? `<img src="${src}" alt="${name}" title="${name}">` : '';
    }).join('');

    //Displays the actual cards from an opened pack
    box.innerHTML = `<div class="cards-scroll">${imgs}</div>`;
}