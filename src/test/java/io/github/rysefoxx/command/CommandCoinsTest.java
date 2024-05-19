package io.github.rysefoxx.command;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import io.github.rysefoxx.PlayLegendQuest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CommandCoinsTest {


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
    public void validCoins() throws InterruptedException {
        this.player.performCommand("coins");

        //Sehr dumm! Aber da alles Async läuft, müssen wir warten, bis der Command ausgeführt wurde.
        Thread.sleep(5000);

        String message = this.player.nextMessage();
        Assertions.assertTrue(message.startsWith("You have"));
    }

}