package io.github.rysefoxx.command.operation;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import io.github.rysefoxx.PlayLegendQuest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

class QuestRewardOperationTest {

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
    public void rewardAddQuestNotFound() throws InterruptedException {
        String questName = UUID.randomUUID().toString().replace("-", "");

        this.player.performCommand("quest reward add " + questName + " 5555234212");

        //Sehr dumm! Aber da alles Async läuft, müssen wir warten, bis der Command ausgeführt wurde.
        Thread.sleep(5000);

        this.player.assertSaid("The quest does not exist.");
    }

    @Test
    public void rewardRemoveQuestNotFound() throws InterruptedException {
        String questName = UUID.randomUUID().toString().replace("-", "");

        this.player.performCommand("quest reward remove " + questName + " 5555234212");

        //Sehr dumm! Aber da alles Async läuft, müssen wir warten, bis der Command ausgeführt wurde.
        Thread.sleep(5000);

        this.player.assertSaid("The quest does not exist.");
    }

    @Test
    public void rewardAddRewardNotFound() throws InterruptedException {
        String questName = UUID.randomUUID().toString().replace("-", "");
        this.player.performCommand("quest create " + questName);

        //Sehr dumm! Aber da alles Async läuft, müssen wir warten, bis der Command ausgeführt wurde.
        Thread.sleep(5000);

        this.player.assertSaid("The quest has been successfully created.");
        this.player.performCommand("quest reward add " + questName + " 5555234212");

        //Sehr dumm! Aber da alles Async läuft, müssen wir warten, bis der Command ausgeführt wurde.
        Thread.sleep(5000);

        this.player.assertSaid("The reward does not exist.");
    }

    @Test
    public void rewardRemoveRewardNotFound() throws InterruptedException {
        String questName = UUID.randomUUID().toString().replace("-", "");
        this.player.performCommand("quest create " + questName);

        //Sehr dumm! Aber da alles Async läuft, müssen wir warten, bis der Command ausgeführt wurde.
        Thread.sleep(5000);

        this.player.assertSaid("The quest has been successfully created.");
        this.player.performCommand("quest reward remove " + questName + " 5555234212");

        //Sehr dumm! Aber da alles Async läuft, müssen wir warten, bis der Command ausgeführt wurde.
        Thread.sleep(5000);

        this.player.assertSaid("The reward does not exist.");
    }

}