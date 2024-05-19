package io.github.rysefoxx.command.operation;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import io.github.rysefoxx.PlayLegendQuest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

class QuestDescriptionOperationTest {

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
    public void questUpdatedLongDescription() throws InterruptedException {
        String questName = UUID.randomUUID().toString().replace("-", "");
        this.player.performCommand("quest create " + questName);

        //Sehr dumm! Aber da alles Async läuft, müssen wir warten, bis der Command ausgeführt wurde.
        Thread.sleep(5000);

        this.player.assertSaid("The quest has been successfully created.");
        this.player.performCommand("quest update description " + questName + " das ist eine unfassbare lange beschreibung");

        //Sehr dumm! Aber da alles Async läuft, müssen wir warten, bis der Command ausgeführt wurde.
        Thread.sleep(5000);

        this.player.assertSaid("The quest has been successfully updated.");
    }

    @Test
    public void questUpdatedShortDescription() throws InterruptedException {
        String questName = UUID.randomUUID().toString().replace("-", "");
        this.player.performCommand("quest create " + questName);

        //Sehr dumm! Aber da alles Async läuft, müssen wir warten, bis der Command ausgeführt wurde.
        Thread.sleep(5000);

        this.player.assertSaid("The quest has been successfully created.");
        this.player.performCommand("quest update description " + questName + " description");

        //Sehr dumm! Aber da alles Async läuft, müssen wir warten, bis der Command ausgeführt wurde.
        Thread.sleep(5000);

        this.player.assertSaid("The quest has been successfully updated.");
    }

    @Test
    public void updateInvalidQuestDescription() throws InterruptedException {
        String questName = UUID.randomUUID().toString().replace("-", "");

        this.player.performCommand("quest update description " + questName + " description");

        //Sehr dumm! Aber da alles Async läuft, müssen wir warten, bis der Command ausgeführt wurde.
        Thread.sleep(5000);

        this.player.assertSaid("The quest does not exist.");
    }

}