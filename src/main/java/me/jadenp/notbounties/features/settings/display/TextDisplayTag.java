package me.jadenp.notbounties.features.settings.display;

import me.jadenp.notbounties.NotBounties;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

public class TextDisplayTag extends TagProvider {

    private TextDisplay textDisplay = null;
    private Team team = null;

    protected TextDisplayTag(Player trackedPlayer) {
        super(trackedPlayer);
    }

    @Override
    public void setText(String text) {
        super.setText(text);
        textDisplay.setText(text);
    }

    @Override
    public String getText() {
        return textDisplay.getText();
    }

    @Override
    public void updateVisibility() {
        if (!trackedPlayer.getPassengers().contains(textDisplay)) {
            Location spawnLocation = trackedPlayer.getEyeLocation();
            spawnLocation.setPitch(0);
            spawnLocation.setYaw(0);
            textDisplay.teleport(spawnLocation);
            trackedPlayer.addPassenger(textDisplay);
        }

        if (trackedPlayer.canSee(textDisplay) && !WantedTags.isShowOwn()) {
            trackedPlayer.hideEntity(NotBounties.getInstance(), textDisplay);
        }

        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager != null && trackedPlayer.getPassengers().size() == 1) {
            spawnNametag(manager);
        }
    }

    @Override
    public void teleport() {
        //textDisplay.teleport(trackedPlayer.getEyeLocation().add(0, WantedTags.getWantedOffset() + 0.3, 0).subtract(trackedPlayer.getVelocity()));
        if (!trackedPlayer.getPassengers().contains(textDisplay)) {
            Location spawnLocation = trackedPlayer.getEyeLocation();
            spawnLocation.setPitch(0);
            spawnLocation.setYaw(0);
            textDisplay.teleport(spawnLocation);
            trackedPlayer.addPassenger(textDisplay);
        }
        lastLocation = textDisplay.getLocation();
    }

    @Override
    public void remove() {
        if (textDisplay != null) {
            trackedPlayer.removePassenger(textDisplay);
            textDisplay.remove();
            textDisplay = null;
        }
        if (team != null) {
            try {
                team.unregister();
            } catch (IllegalStateException ignored) {
                // team already unregistered
            }
            team = null;
        }

    }

    @Override
    public void spawn() {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager != null && trackedPlayer.getPassengers().isEmpty()) {
            spawnNametag(manager);
        }

        Location spawnLocation = trackedPlayer.getEyeLocation();
        spawnLocation.setPitch(0);
        spawnLocation.setYaw(0);
        textDisplay = trackedPlayer.getWorld().spawn(spawnLocation, TextDisplay.class);
        textDisplay.setTransformation(new Transformation(new Vector3f(0f, (float) WantedTags.getWantedOffset() + 0.15f, 0f), new AxisAngle4f(0f, 0f, 0f, 1f), new Vector3f(1f, 1f, 1f), new AxisAngle4f(0f, 0f, 0f, 1f)));
        textDisplay.setBackgroundColor(org.bukkit.Color.fromARGB(64, 0, 0, 0));
        textDisplay.setSeeThrough(false);
        textDisplay.setAlignment(TextDisplay.TextAlignment.CENTER);
        textDisplay.setBillboard(Display.Billboard.VERTICAL);
        if (!WantedTags.isShowOwn()) {
            trackedPlayer.hideEntity(NotBounties.getInstance(), textDisplay);
        }
        lastLocation = textDisplay.getLocation();
        trackedPlayer.addPassenger(textDisplay);

        textDisplay.getPersistentDataContainer().set(NotBounties.getNamespacedKey(), PersistentDataType.STRING, NotBounties.SESSION_KEY);
    }

    private void spawnNametag(ScoreboardManager manager) {
        Scoreboard scoreboard = manager.getMainScoreboard();

        if (scoreboard.getEntryTeam(trackedPlayer.getName()) != null)
            // player already in a team
            return;

        String teamName = "bounty_" + trackedPlayer.getName();
        team = scoreboard.getTeam(teamName);
        if (team == null)
            team = scoreboard.registerNewTeam(teamName);

        team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);
        team.addEntry(trackedPlayer.getName());
        trackedPlayer.setScoreboard(scoreboard);




    }

    @Override
    public boolean isValid() {
        return textDisplay != null;
    }
}
