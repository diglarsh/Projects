import math

import board
from board import Board


class Player:

    def __init__(self, depthLimit, isPlayerOne):

        self.isPlayerOne = isPlayerOne
        self.depthLimit = depthLimit

    # TODO
    # Returns a heuristic for the board position
    # Good positions for 0 pieces should be positive and good positions for 1 pieces
    # should be negative
    def heuristic(self, board):

        #So as to not miss lethal
        winner = board.isTerminal()
        if winner == 1:
            return 1000000 - board.numMoves
        if winner == 2:
            return -1000000 + board.numMoves
        if winner == 0:
            return 0

        heurVal = 0


        for colIndex in range(len(board.board)):
            col = board.board[colIndex]

            #checks for incomplete vertical wins

            if len(col) >= 1: #1 in a row
                topVal = col[len(col) - 1]
                if len(col) >= 2 and col[len(col) - 2] == topVal: #2 in a row
                    if len(col) >= 3 and col[len(col) - 3] == topVal: #3 in a row
                        if len(col) <= 5: #room for a fourth
                            #if topVal == board.numMoves % 2:
                            #    return (1000000-board.numMoves) * self.iterateVal(topVal)
                            heurVal += 4 * self.iterateVal(topVal)
                    elif len(col) <= 4: #room for two more
                        heurVal += 2* self.iterateVal(topVal)
                elif len(col) <= 3: #room for 3 more
                    heurVal += self.iterateVal(topVal)

            #checks for potential horizontal and diagonal wins, starting at the given tile
            for tileIndex in range(len(col)):
                tile = col[tileIndex]

                if colIndex < board.WIDTH - 3:
                    right1 = board.board[colIndex + 1]
                    right2 = board.board[colIndex + 2]
                    right3 = board.board[colIndex + 3]

                    horiz = [tile]
                    diag1 = [tile]
                    diag2 = [tile]

                    if len(right1) <= tileIndex:
                        horiz.append(-1)
                    else:
                        horiz.append(right1[tileIndex])
                    if len(right2) <= tileIndex:
                        horiz.append(-1)
                    else:
                        horiz.append(right2[tileIndex])
                    if len(right3) <= tileIndex:
                        horiz.append(-1)
                    else:
                        horiz.append(right3[tileIndex])

                    if tileIndex <= 2:
                        if len(right1) <= tileIndex + 1:
                            diag1.append(-1)
                        else:
                            diag1.append(right1[tileIndex + 1])
                        if len(right2) <= tileIndex + 2:
                            diag1.append(-1)
                        else:
                            diag1.append(right2[tileIndex + 2])
                        if len(right3) <= tileIndex + 3:
                            diag1.append(-1)
                        else:
                            diag1.append(right3[tileIndex + 3])

                    if tileIndex >= 3:
                        if len(right1) <= tileIndex - 1:
                            diag2.append(-1)
                        else:
                            diag2.append(right1[tileIndex - 1])
                        if len(right2) <= tileIndex - 2:
                            diag2.append(-1)
                        else:
                            diag2.append(right2[tileIndex - 2])
                        if len(right3) <= tileIndex - 3:
                            diag2.append(-1)
                        else:
                            diag2.append(right3[tileIndex - 3])

                    horizVal = self.help(horiz)
                    diag1Val = self.help(diag1)
                    diag2Val = self.help(diag2)

                    if horizVal == 1:
                        heurVal += 4 * self.iterateVal(tile)
                    if horizVal == 2:
                        heurVal += 2 * self.iterateVal(tile)
                    if horizVal == 3:
                        heurVal += self.iterateVal(tile)
                    if diag1Val == 1:
                        heurVal += 4 * self.iterateVal(tile)
                    if diag1Val == 2:
                        heurVal += 2 * self.iterateVal(tile)
                    if diag1Val == 3:
                        heurVal += self.iterateVal(tile)
                    if diag2Val == 1:
                        heurVal += 4 * self.iterateVal(tile)
                    if diag2Val == 2:
                        heurVal += 2 * self.iterateVal(tile)
                    if diag2Val == 3:
                        heurVal += self.iterateVal(tile)

        return heurVal



    #returns the amount of tiles that need to be added to get four in a row,
    # if getting four in a row is impossible, return -1
    def help(self, ls):
        tile = ls[0]
        otherTile = 1 - tile
        length = len(ls)
        if length <= 1 or ls[1] == otherTile or length == 2 or ls[2] == otherTile or length == 3 or ls[3] == otherTile: #other player
            return -1
        count = 0
        for i in ls:
            if i == tile:
                count += 1
        return 4 - count



    #returns 1 if tile = 0 or -1 if tile = 1
    def iterateVal(self, tile):
        return -2 * tile + 1


class PlayerABDP(Player):
    def __init__(self, depthLimit, isPlayerOne):
        super().__init__(depthLimit, isPlayerOne)

        # dictionary for dynamic programming
        self.resolved = {}

    # returns the optimal column to move in by implementing the Alpha-Beta algorithm
    # with dynamic programming


    # returns the optimal column to move in by implementing the Alpha-Beta algorithm
    def findMove(self, board):
        def moveHelp(board, depth, isMax, alpha, beta):

            returnVal = self.resolved.get(board.hash())
            if returnVal:
                return returnVal
            term = board.isTerminal()
            if term == 0 or term == 1 or term == 2 or depth == 0:
                return (self.heuristic(board), board.lastMove[1])
            # find yo kids
            children = board.children()
            # find max/min heuristic for children
            move = -1
            updateAB = lambda alpha, beta, temp, isMax: (max(alpha, temp), beta) if isMax else (alpha, min(beta, temp))

            if isMax:
                mH = -math.inf
                for child in children:
                    if len(board.board[child[0]]) < 6:
                        curHeur = moveHelp(child[1], depth - 1, not isMax, alpha, beta)
                        if curHeur[0] > mH:
                            mH = curHeur[0]
                            move = child[0]
                        alpha, beta = updateAB(alpha, beta, mH, isMax)
                        if alpha >= beta:
                            break
            else:
                mH = math.inf
                minCol = 0
                for child in children:
                    if len(board.board[child[0]]) < 6:
                        curHeur = moveHelp(child[1], depth - 1, not isMax, alpha, beta)
                        if curHeur[0] < mH:
                            mH = curHeur[0]
                            move = child[0]
                        alpha, beta = updateAB(alpha, beta, mH, isMax)
                        if alpha >= beta:
                            break
            returnVal = (mH, move)
            self.resolved[board.hash()] = returnVal
            return returnVal

        score, move = moveHelp(board, self.depthLimit, self.isPlayerOne, -math.inf, math.inf)
        if move == -1:
            children = board.children()
            for child in children:
                if len(board.board[child[0]]) < 6:
                    move = child[0]
                    break
        return move

class PlayerHuman(Player):
    def __init__(self, isPlayerOne):
        super().__init__(0, isPlayerOne)

    def findMove(self, board):
        while True:
            if self.isPlayerOne:
                moveStr = input("Player 1: Which column will you choose? (input a number between 1 and 7)\n")
            else:
                moveStr = input("Player 2: Which column will you choose? (input a number between 1 and 7)\n")
            try:
                move = int(moveStr)
            except ValueError:
                print("Enter a number between 1 and 7")
                continue
            if (not isinstance(move, int)) or move < 1 or move > 7:
                print("Enter a number between 1 and 7")
            elif board.board[move-1] == 6:
                print("That column is full")
            else:
                return move-1

