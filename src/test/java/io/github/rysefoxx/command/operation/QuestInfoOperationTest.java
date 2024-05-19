package io.github.rysefoxx.command.operation;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import io.github.rysefoxx.PlayLegendQuest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

class QuestInfoOperationTest {

    private PlayerMock player;

    @BeforeEach
    public void setUp() {
        ServerMock mockBukkit = MockBukkit.mock();
        MockBukkit.load(PlayLegendQuest.class);
        this.player = mockBukkit.addPlayer();
    }

    @AfterEach
    public void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    public void noQuestActive() throws InterruptedException {
        this.player.performCommand("quest info");

        //Sehr dumm! Aber da alles Async läuft, müssen wir warten, bis der Command ausgeführt wurde.
        Thread.sleep(5000);

        this.player.assertSaid("You have no active quest.");
    }

    @Test
    public void validQuestInfo() throws InterruptedException {
        String questName = UUID.randomUUID().toString().replace("-", "");
        this.player.performCommand("quest create " + questName);

        //Sehr dumm! Aber da alles Async läuft, müssen wir warten, bis der Command ausgeführt wurde.
        Thread.sleep(5000);

        this.player.assertSaid("The quest has been successfully created.");
        this.player.performCommand("quest update duration " + questName + " 1m");
        this.player.performCommand("quest requirement add " + questName + " COLLECT 5 GRASS_BLOCK");

        //Sehr dumm! Aber da alles Async läuft, müssen wir warten, bis der Command ausgeführt wurde.
        Thread.sleep(5000);

        this.player.nextMessage();

        this.player.assertSaid("The quest has been successfully updated.");
        this.player.performCommand("quest accept " + questName);

        //Sehr dumm! Aber da alles Async läuft, müssen wir warten, bis der Command ausgeführt wurde.
        Thread.sleep(5000);

        this.player.assertSaid("You have successfully accepted the quest.");
        this.player.performCommand("quest info");

        //Sehr dumm! Aber da alles Async läuft, müssen wir warten, bis der Command ausgeführt wurde.
        Thread.sleep(5000);

        Assertions.assertNotEquals(this.player.nextMessage(), "You have no active quest.");
    }
}