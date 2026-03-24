#!/bin/bash
# ============================================================
# FULL UI TEST SUITE — Real JavaFX clients + server
# Runs multiple scenarios, captures screenshots at each step
# ============================================================

set -e
cd /home/hungpk/workspace/hamradio

CP="build/classes:deps/sqlite-jdbc-3.45.1.0.jar:deps/slf4j-api-2.0.9.jar:deps/slf4j-nop-2.0.9.jar"
JAVAFX="--module-path javafx-sdk/lib --add-modules javafx.controls,javafx.fxml,javafx.swing"
NATIVE="-Djava.library.path=lib"
CLIENT="com.hamradio.integration.ScriptedTestClient"
SHOTS="test_screenshots"

mkdir -p $SHOTS
rm -f $SHOTS/*.png

echo "╔══════════════════════════════════════════════════════════════════╗"
echo "║            FULL UI TEST SUITE — Real GUI Tests                 ║"
echo "╚══════════════════════════════════════════════════════════════════╝"
echo ""

# ============================================================
# Start server
# ============================================================
echo "━━━ Starting Server ━━━"
java $NATIVE -cp "$CP" com.hamradio.server.HamRadioServer --port 7100 --propagation full &
SERVER_PID=$!
sleep 2
echo "Server PID: $SERVER_PID"
echo ""

cleanup() {
    echo ""
    echo "━━━ Cleanup ━━━"
    kill $SERVER_PID 2>/dev/null
    wait $SERVER_PID 2>/dev/null
    echo "Server stopped"
}
trap cleanup EXIT

run_client() {
    java $JAVAFX $NATIVE -cp "$CP" $CLIENT "$@" 2>&1
}

# ============================================================
# TEST 1: Basic SSB QSO — VK3ABC (Melbourne) → JA1YXP (Tokyo)
# ============================================================
echo "━━━ TEST 1: SSB QSO — VK3ABC → JA1YXP ━━━"

# Client B (receiver) — wait for signal, screenshot, exit
run_client JA1YXP 35.6762 139.6503 7100000 SSB \
    "wait:6000" \
    "screenshot:$SHOTS/01_ssb_rx_ja1yxp.png" \
    "status" "exit" &
PID_B=$!
sleep 1

# Client A (transmitter) — connect, TX, screenshot, exit
run_client VK3ABC -37.8136 144.9631 7100000 SSB \
    "wait:1000" \
    "tx:CQ CQ CQ DE VK3ABC VK3ABC K" \
    "wait:2000" \
    "screenshot:$SHOTS/01_ssb_tx_vk3abc.png" \
    "status" "exit" &
PID_A=$!

wait $PID_A 2>/dev/null
wait $PID_B 2>/dev/null
echo "[TEST 1 DONE]"
echo ""
sleep 1

# ============================================================
# TEST 2: Full SSB Round-Trip — both sides TX and RX
# ============================================================
echo "━━━ TEST 2: Full Round-Trip QSO ━━━"

# Client B — receives first, then transmits response
run_client W1XYZ 42.3601 -71.0589 7100000 SSB \
    "wait:5000" \
    "screenshot:$SHOTS/02_roundtrip_rx_w1xyz.png" \
    "tx:VK3ABC DE W1XYZ UR 59 73 K" \
    "wait:2000" \
    "screenshot:$SHOTS/02_roundtrip_tx_w1xyz.png" \
    "status" "exit" &
PID_B=$!
sleep 1

# Client A — transmits first, waits for response
run_client VK3ABC -37.8136 144.9631 7100000 SSB \
    "wait:1000" \
    "tx:CQ CQ DE VK3ABC K" \
    "wait:2000" \
    "screenshot:$SHOTS/02_roundtrip_tx_vk3abc.png" \
    "wait:5000" \
    "screenshot:$SHOTS/02_roundtrip_rx_vk3abc.png" \
    "status" "exit" &
PID_A=$!

wait $PID_A 2>/dev/null
wait $PID_B 2>/dev/null
echo "[TEST 2 DONE]"
echo ""
sleep 1

# ============================================================
# TEST 3: AM Mode — different modulation
# ============================================================
echo "━━━ TEST 3: AM Mode QSO ━━━"

run_client RX_AM 51.5074 -0.1278 7200000 AM \
    "wait:5000" \
    "screenshot:$SHOTS/03_am_rx.png" \
    "status" "exit" &
PID_B=$!
sleep 1

run_client TX_AM 48.8566 2.3522 7200000 AM \
    "wait:1000" \
    "tx:CQ CQ DE F1ABC ON AM" \
    "wait:2000" \
    "screenshot:$SHOTS/03_am_tx.png" \
    "status" "exit" &
PID_A=$!

wait $PID_A 2>/dev/null
wait $PID_B 2>/dev/null
echo "[TEST 3 DONE]"
echo ""
sleep 1

# ============================================================
# TEST 4: FM Mode
# ============================================================
echo "━━━ TEST 4: FM Mode QSO ━━━"

run_client RX_FM 34.0522 -118.2437 145500000 FM \
    "wait:5000" \
    "screenshot:$SHOTS/04_fm_rx.png" \
    "status" "exit" &
PID_B=$!
sleep 1

run_client TX_FM 37.7749 -122.4194 145500000 FM \
    "wait:1000" \
    "tx:W6ABC DE W6XYZ ON FM" \
    "wait:2000" \
    "screenshot:$SHOTS/04_fm_tx.png" \
    "status" "exit" &
PID_A=$!

wait $PID_A 2>/dev/null
wait $PID_B 2>/dev/null
echo "[TEST 4 DONE]"
echo ""
sleep 1

# ============================================================
# TEST 5: Long Distance — VK3ABC (Melbourne) → ZL1ABC (New Zealand)
#          vs short distance (same city)
# ============================================================
echo "━━━ TEST 5: Distance Comparison ━━━"

# Short distance (same city ~10km)
run_client RX_NEAR 48.8700 2.3700 7100000 SSB \
    "wait:5000" \
    "screenshot:$SHOTS/05_near_rx.png" \
    "status" "exit" &
PID_B=$!
sleep 1

run_client TX_NEAR 48.8566 2.3522 7100000 SSB \
    "wait:1000" \
    "tx:SHORT DISTANCE TEST" \
    "wait:2000" \
    "screenshot:$SHOTS/05_near_tx.png" \
    "status" "exit" &
PID_A=$!

wait $PID_A 2>/dev/null
wait $PID_B 2>/dev/null
echo "[TEST 5 DONE]"
echo ""
sleep 1

# ============================================================
# TEST 6: Multiple rapid transmissions
# ============================================================
echo "━━━ TEST 6: Multiple Transmissions ━━━"

run_client RX_MULTI 35.6762 139.6503 7100000 SSB \
    "wait:12000" \
    "screenshot:$SHOTS/06_multi_rx.png" \
    "status" "exit" &
PID_B=$!
sleep 1

run_client TX_MULTI -37.8136 144.9631 7100000 SSB \
    "wait:1000" \
    "tx:MSG 1 FIRST TRANSMISSION" \
    "wait:1500" \
    "tx:MSG 2 SECOND TRANSMISSION" \
    "wait:1500" \
    "tx:MSG 3 THIRD TRANSMISSION" \
    "wait:2000" \
    "screenshot:$SHOTS/06_multi_tx.png" \
    "status" "exit" &
PID_A=$!

wait $PID_A 2>/dev/null
wait $PID_B 2>/dev/null
echo "[TEST 6 DONE]"
echo ""
sleep 1

# ============================================================
# RESULTS
# ============================================================
echo "╔══════════════════════════════════════════════════════════════════╗"
echo "║                    TEST RESULTS                                ║"
echo "╠══════════════════════════════════════════════════════════════════╣"
echo "║ Screenshots captured:                                          ║"
ls -1 $SHOTS/*.png 2>/dev/null | while read f; do
    SIZE=$(stat -c%s "$f")
    NAME=$(basename "$f")
    printf "║   %-40s %8d bytes  ║\n" "$NAME" "$SIZE"
done
COUNT=$(ls -1 $SHOTS/*.png 2>/dev/null | wc -l)
echo "╠══════════════════════════════════════════════════════════════════╣"
printf "║   Total: %-3d screenshots                                       ║\n" "$COUNT"
echo "╚══════════════════════════════════════════════════════════════════╝"
