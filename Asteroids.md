# Asteroids

You have been provided with code for a singleplayer game. In this assignment you will extend upon this game by adding various functionalities such as multiplayer and persistent highscores.

# Deadlines

There are two deadlines for this assignment:

- **Deadline 1: 20th of October at 23:59**
  This is an optional deadline and is there as an extra opportunity for you to get feedback on your code. As the schedule suggests, there is also an intermediate demo associated with this deadline. If you do not wish to attend this demo (i.e. only create the pull request), you should mention so in your pull request.
- **Deadline 2: 30th of October at 23:59**
  This is the final deadline. Your entire program should be done at this point.

Deadline extensions will not be granted if asked on the same day as the deadline. If you have a good reason that might warrant a deadline extension, contact us as early as possible. For deadline extensions due to illness, we require confirmation from the academic advisor.

# Introduction

Included in this assignment, you'll find a working single-player version of the classic "Asteroids" arcade game, whose objective is to shoot as many asteroids as possible without crashing your ship.

## Out-of-the-box Gameplay & Controls

When you first boot up the game, you'll notice a number in the top-left of your screen, as well as a green energy bar. The number is your score, indicating how many asteroids you've destroyed, and the energy bar shows the status of your ship's onboard capacitor banks. Each time you move the ship or fire your weapon, this will draw some energy from your ship, and when your ship runs out of energy, don't expect your thrusters or weapons to work reliably, so keep an eye on that. Over time though, your ship's solar cells will generate energy, but not fast enough to handle many costly operations at once.

By default, the controls for the game are as follows:

- **Accelerate Forward** - `W`
- **Turn Left** - `A`
- **Turn Right** - `D`
- **Fire Weapon** - `SPACE`

In the top of the game window, you'll see a menu where you have the option to either start a new game, or quit the game.

# Project Structure

Before you begin improving upon this base game, please take some time to browse through the source code. Every class is complete with Javadoc strings for every method and instance variable, but here is a brief description of structure of the project:

- The project as a whole follows the MVC design pattern. Anything pertaining to the game's model is found in the `model` package, view things such as Swing UI components are found in the `view` package, and yes, you guessed it, you'll find controllers in the `control` package.
- The main model containing all the information about the state of the game is `nl.rug.aoop.asteroids.model.Game`. This model consists of a `Spaceship`, some `Bullet` objects and some `Asteroid` objects.
- When starting the game, an `AsteroidsFrame` is created, and it contains an `AsteroidsPanel`. This panel is responsible for drawing all the objects in the game each time the screen refreshes. However, the panel itself doesn't contain the code that draws each object. Instead, you'll find that in the `view.view_models` package. When the panel wants to draw the spaceship, for example, it will construct a new view model for the game's spaceship, and call that view model's `drawObject()` method.
- The actual game loop and physics updates can be found in `GameUpdater`. This class implements `Runnable`, and essentially runs the game's logic in a separate thread, and once in a while asks the asteroids panel to repaint itself.
  - Because we don't want to do what Skyrim did and lock the game physics to the FPS, this game allows you to change the FPS while keeping the physics update rate (tick) the same. In short, this is done by iterating continuously without sleeping, and updating the physics and the display at different intervals, completely independent of each other. However, it's not that simple, since if the FPS is higher than the tick rate, there is a chance that the display will be refreshed twice between ticks. Since no physics update was done in that time, all objects will be in the same position as before. To avoid this issue, we simply assume that the object keeps moving at the same speed it has before, in the same direction as before. By doing this, the game appears smoother when a higher FPS is set. For a more detailed look at how this works, take a look at `GameObjectViewModel::drawObject()`. If you'd like a more in-depth explanation that's beyond the scope of this course, consider [this article](https://gameprogrammingpatterns.com/game-loop.html).

> You are of course allowed to modify the existing code.

# Assignment Information

While some might say this game is already quite impressive, we want you to do better, by making it multiplayer. More specifically, here are some things we're going to look for when grading your project:

- Stable multiplayer functionality across multiple machines using UDP. 
- A main menu from which the user can select at least these five options:

  1. Start singleplayer game.
  2. Join a multiplayer game.
  3. Host a multiplayer game.
  4. Spectate a multiplayer game.
  5. View highscores
  
- Different players'  spaceships should be able to destroy each other with their weapons.
- Each player should have a nickname, and some sort of visual attribute, such as a differently colored ship.
- Persistent high scores, stored in a (local) database. This can be done in an ObjectDb database, SQLite3, MySQL, etc. Since scoring a multiplayer game is up to your interpretation, and because the scores from a multiplayer game may very likely be meaningless from one game to the next (due to a varying number of players, for example), at the very minimum we require a persistent high scores database for all singleplayer games.
- Players (or spectators) connected to a multiplayer game should be able to quit (and not just by calling `System.exit(0);`, don't be lazy). Quitting should bring them back to the main menu.
- The host should still be able to play the game. Hosting is simply playing the game, but also allowing other people to join your game.
- The host should be able to stop hosting the game, causing all connected players to disconnect, sending them back to the main menu.
- Similarly, when a connected player disconnects or quits, the host should not.
- When in a multiplayer game, a player (or spectator) should see the score of all connected players. Whether or not you show players each other's energy levels is up to you.
- When a player (unexpectedly) quits, the host should eventually remove them from the game.
- Ideally the game should work across multiple machines (in the same network).


As per usual, don't forget to properly document your code.

> In previous years, the guidelines were slightly stricter. Since all this does is hinder creativity, we will not tell you how to compute the score of a multiplayer game, or whether or not ships can collide with each other, or a host of other minor details. These things are up to you to decide. See this assignment as a sandbox where the most important part is how you design your program and how well you utilise all the OOP concepts you have learned. 

## Hints

- Think about what information to send between the host and players. You should not be sending too much unnecessary information.
- Think about how often/when you need to send this information.
- Think about a way to handle packets that are lost or arrive out of order.
- The player will need to send different packets to the host (and the other way around). Think of a way to nicely handle these different packets.

# Report

Usually any GitHub repository is accompanied by a `README.md` which explains some of the basic stuff about the code in the repository. Up to now, the `README.md` files have been used as the assignment descriptions. As we want you to have an idea of how to do this and prepare you for the future, you will be required to have such a file in your repository (more specifically, in the directory of this assignment). 

We have already done some of the work for you, so you can find a template markdown file in your repository.  As this is a rather large project, we have also added some other sections to this file that you would not normally find a README. These sections are:

- **Design Description**
- **Evaluation**
- **Teamwork**
- **Extras**

Please fill them in accordingly. We included some expected lengths with each section, but these are by no means set in stone. The general rule is that the section should be long enough so that it discusses what is asked in that specific section. Don't forget that you can (and should) add subsections. 
If you are not familiar with markdown, don't worry as it is extremely easy to learn. A very useful guide can be found [here](https://github.com/adam-p/markdown-here/wiki/Markdown-Cheatsheet)

## Additional info

The template of your README file was taken from [here](https://github.com/othneildrew/Best-README-Template/blob/master/BLANK_README.md). We slightly adjusted this to better fit with this assignment. Be aware that we also removed some stuff that is not very relevant for this assignment (although normally you would still want this in your readme!). Note that there are many different ways to structure your README and this is by no means the perfect one. 

> In previous years this was a pdf report, but this year we decided to use a README file. The purpose of this is not to have you spend a bunch of time on creating the perfect README, but rather to prepare you for future projects where you actually have to add such a file. Hopefully this gives you an idea of what kind of stuff is usually done in the README. Try to focus primarily on the sections mentioned above, as those are _by far_ the most important for this assignment.

# Important

It is very important that you properly document your code! We expect proper use of Javadoc. While testing is highly encouraged, due to the complexity of the assignment and the potential difficulty of the tests, we have decided to make this optional. You can, however, earn bonus points for this. Should you want to test this application, be sure to check out the section on network testing in the reader.

We will be mainly looking at how you design your code and how you use Object-Oriented Programming to achieve your goals. It is more important to properly design your program than to have all kinds of fancy features.

We will also be looking for any code smells throughout your code, so be sure to periodically check your code and refactor it when needed. A list of code smells can be found in the reader. There is also a list of common mistakes in the OOP reader.

# Handing in + Grading

Once you are done, create a pull request from the `asteroids` branch into the `main` branch. Don't forget the `README.md` file. 
Make sure that your code can be run by the following commands from the project root:
```shell
mvn clean compile
mvn exec:java
```

The point distribution for your grade will look as follows:

| Category     				        | Max points    |
| --------------------------------- |:-------------:| 
| Functionality			      	    | 2			    |
| Design & OOP Concepts 			| 5             |
| Clean Code & Code Quality         | 1             |
| Documentation                     | 1             |
| Report/README                     | 1             |
| Bonus         			        | 1             |

Note that functionality and the other parts go hand-in-hand. We can't grade your program properly if there isn't sufficient functionality for us to judge. It is of course easy to submit a perfectly designed `Hello World!` program, but this will not get you any points.


# Extras

With such an open-ended project, there is a lot of room for adding additional features to the program, and these can improve your grade. Here are some ideas to give you a bit of inspiration:

- Different game modes (Multiplayer Co-op, 1v1 duel, and more).
- Improved graphics or audio.
- Different / more weapons on a ship.
- Other game objects besides asteroids, such as rogue AI ships.
- In-game chat or some form of communication.
- A ranking system for multiplayer games. This is rather complex, but if you do something like this, we'll be very impressed.
- Integration with external applications, such as a website that displays high scores.