package io.github.rysefoxx.command;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import io.github.rysefoxx.PlayLegendQuest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CommandQuestRewardTest {

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
    public void questRewardInvalidType() throws InterruptedException {
        this.player.performCommand("questreward create bbb 5");

        //Sehr dumm! Aber da alles Async läuft, müssen wir warten, bis der Command ausgeführt wurde.
        Thread.sleep(5000);

        this.player.assertSaid("This type of reward could not be found.");
    }

    @Test
    public void questRewardValidType() throws InterruptedException {
        this.player.performCommand("questreward create COINS 5");

        //Sehr dumm! Aber da alles Async läuft, müssen wir warten, bis der Command ausgeführt wurde.
        Thread.sleep(5000);

        this.player.assertSaid("The reward has been successfully saved.");
    }

    @Test
    public void questRewardInvalidInput() throws InterruptedException {
        this.player.performCommand("questreward create COINS 5b");

        //Sehr dumm! Aber da alles Async läuft, müssen wir warten, bis der Command ausgeführt wurde.
        Thread.sleep(5000);

        this.player.assertSaid("Your input is invalid.");
    }

    @Test
    public void questRewardValidItemType() throws InterruptedException {
        this.player.performCommand("questreward create ITEMS 5");

        //Sehr dumm! Aber da alles Async läuft, müssen wir warten, bis der Command ausgeführt wurde.
        Thread.sleep(5000);

        this.player.assertSaid("The reward has been successfully saved.");
    }

    @Test
    public void questRewardValidItemTYpeLessParameter() throws InterruptedException {
        this.player.performCommand("questreward create ITEMS");

        //Sehr dumm! Aber da alles Async läuft, müssen wir warten, bis der Command ausgeführt wurde.
        Thread.sleep(5000);

        this.player.assertSaid("The reward has been successfully saved.");
    }

    @Test
    public void questRewardDeleteInvalidInput() throws InterruptedException {
        this.player.performCommand("questreward delete 5b");

        //Sehr dumm! Aber da alles Async läuft, müssen wir warten, bis der Command ausgeführt wurde.
        Thread.sleep(5000);

        this.player.assertSaid("Your input is invalid.");
    }

    @Test
    public void questRewardDeleteNoChanges() throws InterruptedException {
        this.player.performCommand("questreward delete 5345354534");

        //Sehr dumm! Aber da alles Async läuft, müssen wir warten, bis der Command ausgeführt wurde.
        Thread.sleep(5000);

        this.player.assertSaid("The delete operation has not made any changes to the database.");
    }


    @Test
    public void questRewardUpdateInvalidType() throws InterruptedException {
        this.player.performCommand("questreward update 5 bbb 5");

        //Sehr dumm! Aber da alles Async läuft, müssen wir warten, bis der Command ausgeführt wurde.
        Thread.sleep(5000);

        this.player.assertSaid("This type of reward could not be found.");
    }

    @Test
    public void questRewardUpdateValidType() throws InterruptedException {
        this.player.performCommand("questreward update 1 COINS 2");

        //Sehr dumm! Aber da alles Async läuft, müssen wir warten, bis der Command ausgeführt wurde.
        Thread.sleep(5000);

        this.player.assertSaid("The reward has been successfully updated.");
    }

    @Test
    public void questRewardUpdateInvalidInput() throws InterruptedException {
        this.player.performCommand("questreward update 5b COINS 2");

        //Sehr dumm! Aber da alles Async läuft, müssen wir warten, bis der Command ausgeführt wurde.
        Thread.sleep(5000);

        this.player.assertSaid("Your input is invalid.");
    }
}