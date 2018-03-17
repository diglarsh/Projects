from board import Board
from player import PlayerABDP, PlayerHuman

# Daniel Iglarsh
# Worked with Tim Jones



class Game:


    def __init__(self, startBoard, player1, player2):
        self.startBoard = startBoard
        self.player1 = player1
        self.player2 = player2

    ########################################################################
    #                     Simulate a Local Game
    ########################################################################

    def simulateLocalGame(self):

        board = Board(orig=self.startBoard)
        isPlayer1 = True

        while(True):

            #finds the move to make
            if isPlayer1:
                move = self.player1.findMove(board)
            else:
                move = self.player2.findMove(board)

            #makes the move
            board.makeMove(move)
            board.print()

            #determines if the game is over or not
            isOver = board.isTerminal()
            if isOver == 0:
                print("It is a draw!")
                break
            elif isOver == 1:
                print("Player 1 wins!")
                break
            elif isOver == 2:
                print("Player 2 wins!")
                break
            else:
                isPlayer1 = not isPlayer1



if __name__ == "__main__":
    while True:
        p1Human = input("Is player one a human or a computer ('h' or 'c')?\n")
        if p1Human == "h":
            player1 = PlayerHuman(True)
            break
        elif p1Human == "c":
            while True:
                diffStr = input("What difficulty level?\n")
                try:
                    diff = int(diffStr)
                    if diff < 0:
                        raise ValueError
                except ValueError:
                    print("please input a positive whole number")
                    continue
                player1 = PlayerABDP(diff, True)
                break
            break

    while True:
        p2Human = input("Is player two a human or a computer ('h' or 'c')?\n")
        if p2Human == "h":
            player2 = PlayerHuman(False)
            break
        elif p2Human == "c":
            while True:
                diffStr = input("What difficulty level?\n")
                try:
                    diff = int(diffStr)
                    if diff <= 0:
                        raise ValueError
                except ValueError:
                    print("please input a positive whole number")
                    continue
                player2 = PlayerABDP(diff, False)
                break
            break

    b = Game(Board(), player1, player2)
    b.simulateLocalGame()
