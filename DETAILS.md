# 2h Combat Achievements

> The full description of 2h Combat Achievements. The short version, with pictures, is the plugin's page on the Plugin Hub (the repository's README).

A sidebar checklist of every boss and monster in the game's Combat Achievements, with each one's tasks and how many are left. Ones you have finished, and any you mark yourself, show green in the game's own Combat Achievements Tasks menu, and ones that are nearly done show yellow there. You can change the colours and when a name turns yellow.

## The sidebar

Click the plugin's button in RuneLite's sidebar: the Combat Achievements book with the 2h coin, by default.

- **The top card** says how many of the 92 bosses show green, such as "32 of 92 done". Under it, where the green comes from: "Shown green in the Tasks menu", or "Only marked bosses show green" when "Green when all tasks done" is off.
- **Bosses and Manual** switch between the full list and your marks. Manual shows how many marks you have, such as "Manual (3)".
- **The search box** keeps the bosses whose name holds what you type, ignoring case. It matches the game's list name ("Whisperer, The") and the full name ("The Whisperer"), and opens the first match.
- **Each row** is one name from the game's Monster list, in the same order, with:
  - a tick box: ticked means the name shows green in the game's Tasks menu;
  - the name as the game's list writes it, in its colour: green, yellow or orange, unless you chose other colours;
  - where it stands: "3 left", "Done", "Marked" (you marked it) or "No tasks" (the game lists no tasks for it yet).

## A boss's card

Click a boss's name to open it under its row, in a frame. Click it again to close it.

- **Its picture**, from the OSRS wiki, when "Wiki pictures" is on.
- **Its full name and progress**, such as "4 of 9 tasks done".
- **"Green in the Tasks menu"**, the same box as the row's. Under it, nothing while the boss follows your account. Once you tick or untick the box yourself, a tag appears: **MANUAL**, outlined in orange and followed by "overrides your progress" while your mark differs from your account, or in grey with "same as your progress" once it no longer does. The Manual tab lists every mark. Clicking the box back removes an overriding mark. With "Green when all tasks done" off, an unmarked boss reads "Tick to show it green" and a marked one shows the grey tag alone.
- **A Wiki link** to the boss's page on the OSRS wiki, at its Combat Achievements table.
- **Its tasks**, by tier and then name: a tick for each task your account has done, the task's name, and its tier and type, such as "Elite · Speed". Click a task to read its description, with a Wiki link that searches the wiki for it. Click it again to close it.

## The colours in the game

Open Combat Achievements, then Tasks, then the Monster list: the filter that says "All".

- **Green:** every task for that boss is done on your account (the rule the game uses on its Bosses page), or you marked it done. Your mark always wins.
- **Yellow:** a name that is almost done, by the rule you pick in "Yellow when". A boss you marked done is green instead, and a finished boss is never yellow.
  - **1 or 2 tasks left** (the default): at least one task done, and one or two left.
  - **Started:** at least one task done, not all.
  - **Half done:** half the tasks done or more, not all.
  - **Only Grandmaster left:** at least one task done, every task below Grandmaster done, and a Grandmaster task still open.
  - **Off:** never yellow.
- **The game's own orange:** everything else. "All" is never coloured.
- **The Tier list** shows a tier green when the game counts it complete.

The colours are yours to change: green, yellow and "not done" (the orange), in RuneLite's settings or the sidebar's menu. Green and yellow go on the names and on the box that shows your current choice; green also on the complete tiers. A changed "Not done colour" also recolours the unfinished names in the game's lists (the game's own orange is left to the game, so nothing changes until you pick another). Turn the plugin off and the game's own colours come back. Changes show at once, with no restart.

The colour goes on the names in the open list and on the box that shows your current choice. A name keeps its colour while the mouse is over it.

Tasks the game turns off for your Group Ironman size or on Deadman worlds are left out of every count.

## Your marks

A mark is a tick or untick you made yourself.

- **Tick** an unfinished boss to show it green anyway. **Untick** a finished one to keep it orange. Click the box back and the boss follows your account again.
- **Marks are saved per account**, in RuneLite's settings, so every account keeps its own.
- **The Manual tab** lists them. Under each boss a line says what the mark does and how far your account is, such as "Green instead of orange · 1 of 7 done". The × removes the mark. Two groups:
  - **Your marks override your account:** marks that change what the game shows.
  - **Same as your account:** marks your account now gives by itself, such as "Green either way · 5 of 5 done". Remove all clears them.
  - With "Green when all tasks done" off there is one group, "Your marks".
- **A dot on the Manual tab** shows when a mark overrides your account and you have not looked at it yet. Open the Manual tab and it goes. It comes back only for a new one, and each account has its own.

## What counts as done

The plugin reads the task lists the game's Combat Achievements menu uses, and your account's progress on each task, as soon as you log in. It updates when a task is completed. A boss counts as done when every one of its tasks is done.

- **Group Ironman:** tasks the game turns off for your group size are left out, as the game does.
- **Deadman worlds:** tasks the game turns off on Deadman worlds are left out.
- **A name with no tasks yet**, such as the Fractured Archive, turns green only when you tick it.

## Settings

Click the settings icon at the top of the sidebar, or find 2h Combat Achievements in RuneLite's settings. The sidebar's menu has "Yellow when" as a choice, and a "Colours" group: Green..., Yellow..., Not done... and Frame... each open RuneLite's colour picker (a window of its own, so the sidebar stays in view and changes as you drag), and "Reset colours" puts all four back. The menu also has "Hide the sidebar button...": it asks first, then turns "Show the sidebar button" off. Turn it back on in RuneLite's settings.

| Setting | What it does |
|---|---|
| Green when all tasks done | Shows a boss green once every task for it is done. On by default. Off: only the bosses you tick show green. |
| Yellow when | When a name turns yellow: 1 or 2 tasks left (the default), Started, Half done, Only Grandmaster left, or Off. If you had the old "Yellow when almost done" switched off, this starts as Off. |
| Wiki pictures | Shows the wiki's picture of a boss on its card. On by default. |
| Show the sidebar button | Shows the plugin's button in RuneLite's sidebar. On by default. Off: the button is hidden and names follow your account only; your marks are kept for when you turn it back on. |
| Sidebar icon | The sidebar button's picture: 2h coin and book (the default), Book only, or 2h coin and CA letters. |
| Green colour | The colour of finished names, in the sidebar and the Tasks menu. The game's green by default. |
| Yellow colour | The colour of names that are almost done. Yellow by default. |
| Not done colour | The colour of unfinished names. The game's orange by default; another colour is also set on the names in the Tasks menu. |
| Frame colour | The colour of the frame round the boss you opened. A deep teal by default. |

## Privacy

Nothing about your account is sent anywhere. With "Wiki pictures" on, opening a boss sends only its name to oldschool.runescape.wiki, then fetches the picture the wiki names. Pictures are kept in memory only, and no files are written. The Discord, GitHub and Wiki links open in your browser only when you click them. It does not act on the game.

## Licence

BSD 2-Clause. See `LICENSE`.
