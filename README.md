# NotBounties
For support and suggestions, join the [Discord](https://discord.gg/zEsUzwYEx7) server

## Overview
Add bounties to players with *placeholders* or *in-game items* as currency. You can even set bounties on offline players (who have joined the server).
![Bounty GUI](https://i.imgur.com/hWbD9Oh.png)
![Wanted Poster](https://i.imgur.com/IytLGx3.png)

## Features
- Customizable GUIs with [gui.yml](src/main/resources/gui.yml)
- Any number Placeholder or item can be used as currency
- All commands support offline or bedrock players
- MySQL
- Buy permanent scaling, or time immunity to bounties
- Buy your own bounty with interest
- Tax the money used to set bounties
- Tax deaths by bounties
- View other players' bounties and who set them outside the GUI
- Receive a player head when your bounty has been fulfilled
- '&' chat colors, Hex Chat Colors, and Placeholder support in [language.yml](src/main/resources/language.yml)
- Admins can edit or remove bounties with commands or in the GUI
- Set a minimum bounty
- Make bounties expire after x number of days
- Track bounties with a compass
- Top bounty leaderboard
- Big bounty perks
- Leaderboards
- Number formatting
- Whitelist bounties
- Bounty Posters
  
## Config
[Config](src/main/resources/config.yml)

## Use a Custom Currency
Run /bounty currency in-game to change your currency type. The currency can be an item or a placeholder. 

## Commands and Permissions
<details>
  <summary>All Commands and Permissions</summary>
* /bounty help - Shows available commands. no permission.
* /bounty bdc - Toggles the bounty broadcast message. no permission.
* /bounty check (player) - Checks a bounty.notbounties.view
- /bounty list - Lists all bounties. notbounties.view
- /bounty top (all/kills/claimed/deaths/set/immunity) <list> - Lists the top 10 players with the respective stats. notbounties.view
- /bounty stat (all/kills/claimed/deaths/set/immunity) - View your bounty stats. notbounties.view
- /bounty - Opens bounty GUI. notbounties.view
- /bounty (player) (amount) - Adds a bounty to a player. notbounties.set
- /bounty set - Opens bounty-set GUI. notbounties.set
- /bounty buy - Buy your own bounty. notbounties.buyown
- /bounty immunity (price) - Buy immunity to bounties under a certain price. Do not need (price) if permanent immunity is enabled. notbounties.buyimmunity
- /bounty immunity remove - Removes purchased immunity from yourself. notbounties.removeimmunity
- /bounty immunity remove (player) - Removes purchased immunity from a player. notbounties.admin
- /bounty remove (player) - Removes all bounties from a player. notbounties.admin
- /bounty remove (player) from (setter) - Removes a specific bounty put on a player. notbounties.admin
- /bounty edit (player) (amount) - Edits a player's total bounty. notbounties.admin
- /bounty edit (player) from (setter) (amount) - Edits a specific bounty put on a player. notbounties.admin
- /bounty tracker (player) - Gives you a compass that tracks a player with a bounty. notbounties.admin
- /bounty tracker (player) (receiver) - Gives receiver a compass that tracks a player with a bounty. notbounties.admin
- /bounty reload - Reloads the config and language. notbounties.admin
- /bounty currency - Starts setup for the currency - notbounties.admin
- /bounty whitelist (add/remove/set) (whitelisted players) - Change the players that can claim the bounties you set. notbounties.whitelist
- /bounty whitelist <offline> - Opens the set whitelist GUI. notbounties.whitelist
- /bounty whitelist reset - Resets your whitelisted players. notbounties.whitelist
- /bounty whitelist view - Displays your whitelisted players in chat. notbounties.whitelist
- /bounty poster (player) - Gives you a poster of a player's bounty. notbounties.admin
- /bounty poster (player) (receiver) - Gives receiver a poster of a player's bounty. notbounties.admin

notbounties.immune - is immune from having bounties placed on them
notbounties.tracker - allows players to use the bounty tracker (default true)
notbounties.player - all of the basic player permissions
notbounties.basic - use help, tutorial, and broadcast commands
</details>

## Hex in [Language.yml](src/main/resources/language.yml)
![Hex](https://i.imgur.com/Gztr2se.png)

## Placeholders
- Add "_formatted" to the end to add the currency prefix and suffix.
- Add "_full" to the end of top to add what the stat is about.

%notbounties_bounty%
%notbounties_(all/kills/claimed/deaths/set/immunity)%
%notbounties_top\_[x]\_(all/kills/claimed/deaths/set/immunity)%
