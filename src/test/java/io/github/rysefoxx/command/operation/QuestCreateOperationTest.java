package io.github.rysefoxx.command.operation;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import io.github.rysefoxx.PlayLegendQuest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

class QuestCreateOperationTest {

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
    public void createQuest() throws InterruptedException {
        String questName = UUID.randomUUID().toString().replace("-", "");
        this.player.performCommand("quest create " + questName);

        //Sehr dumm! Aber da alles Async läuft, müssen wir warten, bis der Command ausgeführt wurde.
        Thread.sleep(5000);

        this.player.assertSaid("The quest has been successfully created.");
    }

    @Test
    public void createQuestNameTooLong() throws InterruptedException {
        String questName = UUID.randomUUID().toString().repeat(5);
        this.player.performCommand("quest create " + questName);

        //Sehr dumm! Aber da alles Async läuft, müssen wir warten, bis der Command ausgeführt wurde.
        Thread.sleep(5000);

        this.player.assertSaid("The name of the quest may be a maximum of 40 characters long.");
    }

    @Test
    public void createQuestAlreadyExists() throws InterruptedException {
        String questName = UUID.randomUUID().toString().replace("-", "");
        this.player.performCommand("quest create " + questName);

        //Sehr dumm! Aber da alles Async läuft, müssen wir warten, bis der Command ausgeführt wurde.
        Thread.sleep(5000);

        this.player.assertSaid("The quest has been successfully created.");
        this.player.performCommand("quest create " + questName);

        //Sehr dumm! Aber da alles Async läuft, müssen wir warten, bis der Command ausgeführt wurde.
        Thread.sleep(5000);

        this.player.assertSaid("The quest already exists.");
    }
}