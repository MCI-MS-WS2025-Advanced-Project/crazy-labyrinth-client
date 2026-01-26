package mci.dibse.crazylabyrinth.client.service.animation;

import java.util.Map;
import java.util.Objects;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import labyrinth.contracts.models.BonusType;
import labyrinth.contracts.models.Direction;
import labyrinth.contracts.models.TurnState;
import lombok.extern.slf4j.Slf4j;
import mci.dibse.crazylabyrinth.client.view.viewmodel.GameViewModel;
import mci.dibse.crazylabyrinth.client.view.viewmodel.item.CoordinatesItem;
import mci.dibse.crazylabyrinth.client.view.viewmodel.item.PlayerStateItem;

/**
 * Service for initializing and managing animation listeners in the game.
 * This service decouples animation initialization from the GameStateController
 * to improve readability and maintainability.
 */
@Slf4j
public class AnimationInitializerService {

    private final AnimationService animationService;
    private final GameViewModel gameViewModel;
    private final Map<String, ImageView> idToPlayerPawn;
    private final GridPane boardGrid;

    /**
     * Constructor for AnimationInitializerService.
     *
     * @param animationService the animation service to use for executing animations
     * @param gameViewModel the game view model containing game state
     * @param idToPlayerPawn map of player IDs to their pawn image views
     * @param boardGrid the game board grid pane
     */
    public AnimationInitializerService(
            AnimationService animationService,
            GameViewModel gameViewModel,
            Map<String, ImageView> idToPlayerPawn,
            GridPane boardGrid) {
        this.animationService = animationService;
        this.gameViewModel = gameViewModel;
        this.idToPlayerPawn = idToPlayerPawn;
        this.boardGrid = boardGrid;
    }

    /**
     * Initialize all animation listeners.
     * This method sets up listeners for tile pushes, player movements,
     * and bonus changes that trigger animations.
     */
    public void initializeAnimationListeners() {
        initTilePushListener();
        initPlayerMovementListener();
        initAvailableBonusTypesListener();
    }

    /**
     * Set up listener to animate tile pushes on updates.
     */
    private void initTilePushListener() {
        gameViewModel
                .getGameBoardItem()
                .lastPushProperty()
                .addListener(
                        (obs, oldPush, newPush) -> {
                            if (newPush != null && !Objects.equals(oldPush, newPush)) {
                                int index = newPush.getRowOrColIndex();
                                Direction direction = newPush.getDirection();
                                animationService.animateTilePush(index, direction, boardGrid);
                            }
                        });
    }

    /**
     * Set up listeners to animate player movement updates.
     */
    private void initPlayerMovementListener() {
        gameViewModel
                .getPlayerStatesSorted()
                .forEach(
                        playerStateItem -> {
                            playerStateItem
                                    .getCurrentPosition()
                                    .addListener(
                                            (obs, oldPos, newPos) -> {
                                                if (gameViewModel
                                                        .getCurrentTurnState()
                                                        .equals(TurnState.WAITING_FOR_MOVE)) {
                                                    animationService.animatePlayerPath(
                                                            gameViewModel.getGameBoardItem(),
                                                            idToPlayerPawn.get(
                                                                    playerStateItem
                                                                            .getPlayerIdAsString()),
                                                            oldPos,
                                                            newPos,
                                                            boardGrid);
                                                } else {
                                                    checkForSwapAnimation(
                                                            playerStateItem, oldPos, newPos);
                                                }
                                            });
                        });
    }

    /**
     * Set up listener to animate beam effects when bonus types change.
     */
    private void initAvailableBonusTypesListener() {
        gameViewModel
                .getPlayerStatesSorted()
                .forEach(
                        playerStateItem -> {
                            playerStateItem
                                    .getAvailableBonusTypes()
                                    .addListener(
                                            (obs, oldTypes, newTypes) -> {
                                                if (oldTypes.contains(BonusType.BEAM)
                                                        && !newTypes.contains(BonusType.BEAM)) {
                                                    animationService.animateBeam(
                                                            idToPlayerPawn.get(
                                                                    playerStateItem
                                                                            .getPlayerIdAsString()),
                                                            playerStateItem
                                                                    .getPreviousPosition()
                                                                    .get(),
                                                            playerStateItem
                                                                    .getCurrentPosition()
                                                                    .get(),
                                                            boardGrid);
                                                }
                                            });
                        });
    }

    /**
     * Check if a position change represents a swap between two players.
     *
     * @param movedPlayer the player that moved
     * @param oldPosition the old position of the moved player
     * @param newPosition the new position of the moved player
     */
    private void checkForSwapAnimation(
            PlayerStateItem movedPlayer, CoordinatesItem oldPosition, CoordinatesItem newPosition) {
        gameViewModel.getPlayerStatesSorted().stream()
                .filter(
                        otherPlayer ->
                                !otherPlayer
                                        .getPlayerIdAsString()
                                        .equals(movedPlayer.getPlayerIdAsString()))
                .filter(otherPlayer -> otherPlayer.getPreviousPosition().get() != null)
                .filter(
                        otherPlayer ->
                                otherPlayer.getCurrentPosition().get() != null
                                        && otherPlayer
                                                .getCurrentPosition()
                                                .get()
                                                .equals(oldPosition)
                                        && otherPlayer
                                                .getPreviousPosition()
                                                .get()
                                                .equals(newPosition))
                .findFirst()
                .ifPresent(
                        swapPartner -> {
                            ImageView pawn1 = idToPlayerPawn.get(movedPlayer.getPlayerIdAsString());
                            ImageView pawn2 = idToPlayerPawn.get(swapPartner.getPlayerIdAsString());

                            if (pawn1 != null && pawn2 != null) {
                                animationService.animateSwap(
                                        pawn1, pawn2, oldPosition, newPosition, boardGrid);
                            }
                        });
    }

    /**
     * Check if any animation is currently in progress.
     *
     * @return true if an animation is running, false otherwise
     */
    public boolean isAnimationInProgress() {
        return animationService.isAnimationInProgress();
    }
}
