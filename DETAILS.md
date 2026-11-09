# 2h CA Boss Checklist

> The full description of 2h CA Boss Checklist. The short version, with pictures, is the plugin's page on the Plugin Hub (the repository's README).

A sidebar checklist of every boss in the game's Combat Achievements, with each boss's tasks and how many are left. Bosses you have finished, and any you mark yourself, show green in the game's own Combat Achievements Tasks menu, and bosses with one or two tasks left show yellow there.

## The sidebar

Click the plugin's button in RuneLite's sidebar: the Combat Achievements book with the 2h coin, by default.

- **The top card** says how many of the 92 bosses show green, such as "32 of 92 done". Under it, where the green comes from: "Shown green in the Tasks menu", or "Only marked bosses show green" when "Green when all tasks done" is off. When a mark disagrees with your account, it says so, with a Review link to the Manual tab.
- **Bosses and Manual** switch between the full list and your marks. Manual shows how many marks you have, such as "Manual (3)".
- **The search box** keeps the bosses whose name holds what you type, ignoring case. It matches the game's list name ("Whisperer, The") and the full name ("The Whisperer"), and opens the first match.
- **Each row** is one name from the game's Monster list, in the same order, with:
  - a tick box: ticked means the name shows green in the game's Tasks menu;
  - the name as the game's list writes it;
  - where it stands: "3 left", "Done", "Marked" (you marked it) or "No tasks" (the game lists no tasks for it yet).

## A boss's card

Click a boss's name to open it under its row. Click it again to close it.

- **Its picture**, from the OSRS wiki, when "Wiki pictures" is on.
- **Its full name and progress**, such as "4 of 9 tasks done".
- **"Green in the Tasks menu"**, the same box as the row's. Under it, where the green comes from: "From your account", "Marked by you", or "Tick to show it green" when "Green when all tasks done" is off.
- **Its tasks**, by tier and then name: a tick for each task your account has done, the task's name, and its tier and type, such as "Elite · Speed". Click a task to read its description, with a Wiki link that searches the wiki for it. Click it again to close it.

## The colours in the game

Open Combat Achievements, then Tasks, then the Monster list: the filter that says "All".

- **Green:** every task for that boss is done on your account (the rule the game uses on its Bosses page), or you marked it done. Your mark always wins.
- **Yellow:** one or two tasks left, with at least one done. A boss you marked done is green instead.
- **The game's own orange:** everything else. "All" is never coloured.
- **The Tier list** shows a tier green when the game counts it complete.

The colour goes on the names in the open list, not on the box that shows your current choice. It is applied every frame, because the game resets a name's colour while the mouse is over the list.

## Your marks

A mark is a tick or untick you made yourself.

- **Tick** an unfinished boss to show it green anyway. **Untick** a finished one to keep it orange. Click the box back and the boss follows your account again.
- **Marks are saved per account**, in RuneLite's settings, so every account keeps its own.
- **The Manual tab** lists them under "Your marks win over your account", in three groups:
  - **Disagree with your account:** marks made while "Green when all tasks done" was off that your account now disagrees with. Keep makes one a lasting mark; Remove drops it.
  - **Not needed:** marks your account now gives by itself. Remove all clears them.
  - **Your marks:** every other mark, each with Remove.

## What counts as done

The plugin reads the task lists the game's Combat Achievements menu uses, and your account's progress on each task, as soon as you log in. It updates when a task is completed. A boss counts as done when every one of its tasks is done.

- **Group Ironman:** tasks the game turns off for your group size are left out, as the game does.
- **Deadman worlds:** tasks the game turns off on Deadman worlds are left out.
- **The Fractured Archive** has no tasks yet, so it turns green only when you tick it.

## Settings

Click the settings icon at the top of the sidebar, or find 2h CA Boss Checklist in RuneLite's settings.

| Setting | What it does |
|---|---|
| Green when all tasks done | Shows a boss green once every task for it is done. On by default. Off: only the bosses you tick show green. |
| Yellow when almost done | Shows a name yellow in the Tasks menu when one or two of its tasks are left. On by default. |
| Wiki pictures | Shows the wiki's picture of a boss on its card. On by default. |
| Sidebar icon | The sidebar button's picture: 2h coin and book (the default), Book only, or 2h coin and CA letters. |

## Privacy

Nothing about your account is sent anywhere. With "Wiki pictures" on, opening a boss sends only its name to oldschool.runescape.wiki, then fetches the picture the wiki names. Pictures are kept in memory only, and no files are written. The Discord and GitHub links open in your browser only when you click them. It does not act on the game.

## Licence

BSD 2-Clause. See `LICENSE`.
