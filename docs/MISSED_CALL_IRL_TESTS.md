# Missed-call behaviour — in-real-life (device) test list

Manual test checklist for the missed-call surfaces on a **physical device**, since missed-call
state derives from the real call log and can't be seeded easily. Run through this after each port
slice lands. Each test says which **slice** delivers it and whether it's testable **now**.

## Setup / props

- **WandasPhone device** — WandasPhone installed and set as default Home + Phone (so it has call-log
  access). Kiosk on.
- **A "caller" phone** — a second phone to call the WandasPhone device and to answer/not-answer its
  call-backs. Have a **third** number too (or withhold caller ID) for the unknown/withheld cases.
- **Contacts / layout (carer settings):**
  - **Anne** = a contact placed on a **home slot** (a "helper"/home call button).
  - **Katie** = a saved contact **not** on a home slot (a non-helper, e.g. a grandchild).
  - An **unknown** number = the caller phone's number saved in **neither** contact.
  - Place the **missed-call return button** and the **missed-calls list button** on the home layout.
  - Missed-call **nag** enabled.
- **How to produce each event:** *miss* = let it ring out / don't answer; *declined* = reject on the
  WandasPhone; *no-answer call-back* = tap to call back, let the caller phone not answer; *connect* =
  answer the call-back; *withheld* = call with caller ID withheld (dial `141` prefix in the UK).

Legend: ✅ testable now · ⏳ pending the named slice.

---

## A. Unknown callers never show a raw number  — Slice 1 ✅

- [ ] **A1** Unknown number calls and is missed → **button** reads `Unknown` (not the digits);
  **status** reads `Missed call from Unknown`; **list** row reads `Unknown`.
- [ ] **A2** Open the list, tap the unknown row to call back → the "Calling…" screen says
  `Calling Unknown`, **never** the raw number. The correct number is actually dialled.

## B. Status line and button never name different people  — Slice 2 ✅

- [ ] **B1** Anne (helper) misses, then Katie (non-helper contact) misses **later** → status says
  the Anne nag (`…Call Anne now.`) **and** the button shows **Anne** (not Katie). *(Before slice 2
  the button showed Katie — the bug this fixes.)*
- [ ] **B2** Only Katie (non-helper) misses → status `Missed call from Katie` **and** button `Katie`.
- [ ] **B3** Only an unknown misses → status `Missed call from Unknown` **and** button `Unknown`.
- [ ] **B4** No outstanding misses → button `No Missed Calls`, status `[Name]'s phone`.
- [ ] **B5** Anne (helper) is the only miss → button `Anne`, and the nag says Anne. They agree.

## C. Clearing — helper reassurance vs done-my-part  — Slice 3 ⏳

- [ ] **C1** Anne (helper) misses; tap the button to call back; caller phone does **not** answer →
  Anne **stays** on the button and the nag **keeps** going (reassurance). *(Pending slice 3 —
  currently clears on the dial attempt.)*
- [ ] **C2** Anne (helper) misses; call back and it **connects** → clears from button, status, nag. ✅
- [ ] **C3** Katie / unknown misses; tap to call back, **no answer** → drops off the button and the
  list (she's done her part). ✅ *(already the behaviour)*
- [ ] **C4** After any clear, a **new** incoming miss from the same person re-arms it. ✅

## D. Declined calls & ageing  — Slice 4 ⏳

- [ ] **D1** Decline a call from **Anne/Katie** (known) → it surfaces as outstanding (button/list). ✅
- [ ] **D2** Decline a call from an **unknown** number → it does **not** surface (no scam nag).
  *(Pending slice 4 — currently an unknown decline surfaces.)*
- [ ] **D3** A declined-unknown does not linger indefinitely. *(Pending slice 4 — today only MISSED
  rows age out, declines don't.)*

## E. The missed-calls list  — general ✅

- [ ] **E1** List shows outstanding missed **and** declined callers, one row per person, most recent
  first. Tapping a row calls back then returns home.
- [ ] **E2** A **withheld / no-number** miss is **not** in the list and **not** in the count.
- [ ] **E3** The list-button count (`N Missed Calls`) equals the number of rows in the list.

## F. Never strand the senior / carer safeguard  — Slice 5 ⏳ (+ existing ✅)

- [ ] **F1** With the missed-call return button **removed** from the layout, a non-helper/unknown
  miss does **not** put a name on the status line (only a home-slot helper does). ✅
- [ ] **F2** Removing **both** missed buttons from the layout shows the carer a warning that misses
  from people not on the home page can't be returned. *(Pending slice 5.)*

## G. Safety / regression (run every slice) ✅

- [ ] **G1** Tapping the missed-call button dials the **correct** number even when it shows `Unknown`.
- [ ] **G2** **Emergency call-back**: after an emergency/999 call, a call-back from a **different
  unlisted number** appears on the button/list and can be returned. *(Key reason unknowns stay
  callable.)*
- [ ] **G3** Placing / receiving a normal call, and ending it, returns to the home screen; no
  stranded UI; status returns to standby.
- [ ] **G4** Nothing shows a raw phone number on any senior-facing screen at any point.

---

### Slice status
- **Slice 1** (mask unknowns) — PR #1.
- **Slice 2** (helper-first button/status) — this branch.
- **Slice 3** (helper clears on connect, not on dial) — pending; touches the call flow, test C1/C2
  carefully on device before merge.
- **Slice 4** (declined-known-only + declines age out) — pending.
- **Slice 5** (no-return-path warning) — pending.
