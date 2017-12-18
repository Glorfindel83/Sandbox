package nl.magnus.test;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

public class Hex {
	public static void main(String[] args) throws Exception {
		// This is the image from the Puzzling.SE question.
		// Hexominoes are numbered 0-34 (in the hexatridecimal number system)
		String[] source = { //
				" 022225555578AAAACCEEEEEFHHIIJKKMMMMOOORRRRTUUVVVVYY ",
				"0021233466578888AACCDEFFFGHIJJKKLMMNOPORQSRTTUUWVVYYY",
				"00111344467778999CCDDDDFFGHIIJKKLNNNOPPQQSSSTTUWWWWXY",
				"011333446667999BBBBBBDGGGGHHIJJLLLLNNPPPQQQSSTUWXXXXX" };
		int fixedCount = 0, oneSidedCount = 0;
		for (int i = 0; i < freeCount; i++) {
			// Determine character in source image
			char c = (char)(i > 9 ? ('A' + i - 10) : ('0' + i));
			List<Board> boards = new ArrayList<>();
			allBoards.add(boards);
			allParts.add(new ArrayList<>());

			// Find left-most and top-most occurrence of the character
			int minIndex = Byte.MAX_VALUE, minRow = -1;
			for (int row = 0; row < 4; row++) {
				int index = source[row].indexOf(c);
				if (index != -1) {
					if (minRow == -1)
						minRow = row;
					if (index < minIndex)
						minIndex = index;
				}
			}

			// Board representation (original)
			BitBoard board = new BitBoard();
			for (int row = minRow; row < 4; row++) {
				int index = minIndex;
				do {
					index = source[row].indexOf(c, index);
					if (index == -1)
						break;
					board.set(row - minRow, index - minIndex);
					index++;
				} while (true);
			}
			oneSidedCount++;
			fixedCount++;
			determineShifts(board, boards);

			// 90 degrees
			BitBoard board90 = new BitBoard(board);
			board90.rotate();
			fixedCount++;
			determineShifts(board90, boards);
			// (cannot be identical to original with 6 tiles)

			// 180 degrees
			BitBoard board180 = new BitBoard(board90);
			board180.rotate();
			boolean hasRotationalSymmetry = board.equals(board180);
			if (!hasRotationalSymmetry) {
				fixedCount++;
				determineShifts(board180, boards);
			}

			// 270 degrees
			BitBoard board270 = new BitBoard(board180);
			board270.rotate();
			if (!hasRotationalSymmetry) {
				fixedCount++;
				determineShifts(board270, boards);
			}

			// Mirror
			BitBoard mirror = new BitBoard(board);
			mirror.mirror();
			boolean hasMirrorSymmetry = board.equals(mirror) || board90.equals(mirror) || board180.equals(mirror)
					|| board270.equals(mirror);
			if (!hasMirrorSymmetry) {
				oneSidedCount++;
				fixedCount++;
				determineShifts(mirror, boards);
			}

			// Mirror, 90 degrees
			if (!hasMirrorSymmetry) {
				BitBoard mirror90 = new BitBoard(mirror);
				mirror90.rotate();
				fixedCount++;
				determineShifts(mirror90, boards);

				if (!hasRotationalSymmetry) {
					// Mirror, 180 degrees
					BitBoard mirror180 = new BitBoard(mirror90);
					mirror180.rotate();
					fixedCount++;
					determineShifts(mirror180, boards);

					// Mirror, 270 degrees
					BitBoard mirror270 = new BitBoard(mirror180);
					mirror270.rotate();
					fixedCount++;
					determineShifts(mirror270, boards);
				}
			}
		}

		// Check algorithm, by comparing the one-sided and fixed counts with those from Wikipedia
		if (oneSidedCount != 60 || fixedCount != 216) {
			throw new AssertionError("Wrong one-sided / fixed counts.");
		}

		// Check tiling finder, by seeing if the shape tiling mentioned in the question can be found
		if (!findTiling(new byte[] { 18, 7, 5, 8, 11 }, false)) {
			throw new AssertionError("Shape tiling not found.");
		}

		// Find all possible tilings of the shape
		long start = System.currentTimeMillis();
		int shapeTilings = 0;
		for (byte i = 0; i < freeCount; i++) {
			for (byte j = (byte)(i + 1); j < freeCount; j++) {
				for (byte k = (byte)(j + 1); k < freeCount; k++) {
					for (byte l = (byte)(k + 1); l < freeCount; l++) {
						for (byte m = (byte)(l + 1); m < freeCount; m++) {
							if (findTiling(new byte[] { i, j, k, l, m }, false)) {
								// Shape tiling possible, store it in a way which is convenient for later
								BitSet bitSet = new BitSet(freeCount);
								bitSet.set(i);
								bitSet.set(j);
								bitSet.set(k);
								bitSet.set(l);
								bitSet.set(m);
								allParts.get(i).add(bitSet);
								allParts.get(j).add(bitSet);
								allParts.get(k).add(bitSet);
								allParts.get(l).add(bitSet);
								allParts.get(m).add(bitSet);
								System.out.println(i + " " + j + " " + k + " " + l + " " + m);
								shapeTilings++;
							}
						}
					}
				}
			}
		}
		System.out.println("There are " + shapeTilings + " possible shape tilings (found in "
				+ (System.currentTimeMillis() - start) / 1000 + " seconds)");

		// Try to cover the entire range 0 .. 34 with parts for which a shape tiling exists
		start = System.currentTimeMillis();
		int step = 0, nextNumber = 0;
		for (BitSet part : allParts.get(nextNumber)) {
			usedBitSets[step] = part;
			BitSet nextBitSet = (BitSet)part.clone();
			// Find next not-covered number
			nextCoverStep(nextBitSet, nextNumber, step + 1);
		}
		System.out.println("Cover search completed in " + (System.currentTimeMillis() - start) / 1000 + " seconds");
	}

	private static final int freeCount = 35; // found on Wikipedia
	private static List<List<Board>> allBoards = new ArrayList<>();

	private static List<List<BitSet>> allParts = new ArrayList<>();
	private static BitSet[] usedBitSets = new BitSet[7];

	private static boolean findTiling(byte[] tileNumbers, boolean showBoards) {
		for (Board board0 : allBoards.get(tileNumbers[0])) {
			Board boardAfter0 = (Board)board0.clone();
			for (Board board1 : allBoards.get(tileNumbers[1])) {
				if (board1.intersects(boardAfter0))
					continue;
				Board boardAfter1 = boardAfter0.union(board1);
				for (Board board2 : allBoards.get(tileNumbers[2])) {
					if (board2.intersects(boardAfter1))
						continue;
					Board boardAfter2 = boardAfter1.union(board2);
					for (Board board3 : allBoards.get(tileNumbers[3])) {
						if (board3.intersects(boardAfter2))
							continue;
						Board boardAfter3 = boardAfter2.union(board3);
						for (Board board4 : allBoards.get(tileNumbers[4])) {
							if (!board4.intersects(boardAfter3)) {
								if (showBoards) {
									System.out.println(board0);
									System.out.println(board1);
									System.out.println(board2);
									System.out.println(board3);
									System.out.println(board4);
								}
								return true;
							}
						}
					}
				}
			}
		}
		return false;
	}

	private static void determineShifts(BitBoard bitBoard, List<Board> boards) {
		Board board = new Board(bitBoard);
		Board newBoard = board;
		while (newBoard != null) {
			Board newBoard2 = newBoard;
			while (newBoard2 != null) {
				if (!newBoard2.intersects(Board.SHAPE_TEMPLATE))
					boards.add(newBoard2);
				// Shift down
				newBoard2 = newBoard2.shiftDown();
			}
			// Shift right
			newBoard = newBoard.shiftRight();
		}
	}

	private static void nextCoverStep(BitSet currentBitSet, int currentNumber, int step) {
		int nextNumber = currentBitSet.nextClearBit(currentNumber + 1);
		if (nextNumber == freeCount) {
			// Complete tiling found, print it
			for (int i = 0; i < 7; i++) {
				System.out.println(usedBitSets[i]);
				findTiling(getIndices(usedBitSets[i]), true);
			}
			return;
		}
		// Try all parts (for which a tiling has been found) containing the next free number
		for (BitSet part : allParts.get(nextNumber)) {
			if (part.intersects(currentBitSet))
				continue;
			usedBitSets[step] = part;
			BitSet nextBitSet = (BitSet)currentBitSet.clone();
			nextBitSet.or(part);
			nextCoverStep(nextBitSet, nextNumber, step + 1);
		}
	}

	private static byte[] getIndices(BitSet bitSet) {
		byte[] indices = new byte[5];
		int k = 0;
		for (int j = bitSet.nextSetBit(0); j != -1; j = bitSet.nextSetBit(j + 1)) {
			indices[k++] = (byte)j;
		}
		return indices;
	}

	/**
	 * Mutable board, storing everything as a boolean (so less efficient).
	 */
	static class BitBoard {
		public BitBoard() {
			board = new boolean[6][6];
		}

		public BitBoard(BitBoard bitBoard) {
			boolean[][] newBoard = new boolean[6][6];
			for (int row = 0; row < 6; row++) {
				System.arraycopy(bitBoard.board[row], 0, newBoard[row], 0, 6);
			}
			board = newBoard;
		}

		private final boolean[][] board;

		public void set(int row, int column) {
			board[row][column] = true;
		}

		public boolean get(int row, int column) {
			return board[row][column];
		}

		/**
		 * Rotates the board 90 degrees clockwise, and shifts the contents towards the origin.
		 */
		public void rotate() {
			// Create temporary board
			boolean[][] temporaryBoard = new boolean[11][11];

			// Rotate
			for (int y = 0; y < 6; y++) {
				for (int x = 0; x < 6; x++) {
					temporaryBoard[y][x] = board[5 - x][y];
				}
			}

			// Determine shift values
			int minX = -1, minY = -1;
			for (int y = 0; y < 6; y++) {
				for (int x = 0; x < 6; x++) {
					if (temporaryBoard[y][x]) {
						minY = y;
						break;
					}
				}
				if (minY != -1)
					break;
			}
			if (minY == -1)
				return; // empty board, so no changes
			for (int x = 0; x < 6; x++) {
				for (int y = 0; y < 6; y++) {
					if (temporaryBoard[y][x]) {
						minX = x;
						break;
					}
				}
				if (minX != -1)
					break;
			}

			// Shift and copy to main board
			for (int y = 0; y < 6; y++) {
				for (int x = 0; x < 6; x++) {
					board[y][x] = temporaryBoard[y + minY][x + minX];
				}
			}
		}

		/**
		 * Mirrors the board along the main diagonal.
		 */
		public void mirror() {
			for (int x = 0; x < 6; x++) {
				for (int y = x + 1; y < 6; y++) {
					boolean temp = board[y][x];
					board[y][x] = board[x][y];
					board[x][y] = temp;
				}
			}
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			for (int y = 0; y < 6; y++) {
				for (int x = 0; x < 6; x++) {
					builder.append(board[y][x] ? 'X' : '.');
				}
				builder.append('\n');
			}
			return builder.toString();
		}

		@Override
		public boolean equals(Object object) {
			if (this == object)
				return true;
			if (!(object instanceof BitBoard))
				return false;
			for (int y = 0; y < 6; y++) {
				for (int x = 0; x < 6; x++) {
					if (board[y][x] != ((BitBoard)object).board[y][x])
						return false;
				}
			}
			return true;
		}
	}

	/**
	 * Immutable board, stored in the most efficient way.
	 */
	static class Board implements Cloneable {
		public Board(BitBoard bitBoard) {
			rows = new byte[6];
			for (int y = 0; y < 6; y++) {
				for (int x = 0; x < 6; x++) {
					if (bitBoard.get(y, x)) {
						rows[y] += (1 << x);
					}
				}
			}
		}

		private Board(byte[] rows) {
			this.rows = rows;
		}

		private final byte[] rows;

		public static final Board SHAPE_TEMPLATE = new Board(new byte[] { 7, 3, 1, 0, 0, 0 });

		/**
		 * Returns a board where everything is shifted one position to the right
		 * 
		 * @return <code>null</code> if right shift is not possible.
		 */
		public Board shiftRight() {
			byte[] newRows = new byte[6];
			for (int row = 0; row < 6; row++) {
				if (rows[row] >= 32)
					// shift not possible
					return null;
				newRows[row] = (byte)(rows[row] * 2);
			}
			return new Board(newRows);
		}

		/**
		 * Returns a board where everything is shifted one position to the bottom
		 * 
		 * @return <code>null</code> if right shift is not possible.
		 */
		public Board shiftDown() {
			if (rows[5] != 0)
				// shift not possible
				return null;
			byte[] newRows = new byte[6];
			for (int row = 1; row < 6; row++) {
				newRows[row] = rows[row - 1];
			}
			return new Board(newRows);
		}

		/**
		 * Check if two boards have a set bit in common.
		 */
		public boolean intersects(Board board) {
			for (int row = 0; row < 6; row++) {
				if ((rows[row] & board.rows[row]) != 0)
					return true;
			}
			return false;
		}

		/**
		 * Overlays another board (but does not check for possible intersection).
		 */
		public Board union(Board board) {
			byte[] newRows = new byte[6];
			for (int row = 0; row < 6; row++) {
				newRows[row] = (byte)(rows[row] | board.rows[row]);
			}
			return new Board(newRows);
		}

		@Override
		public Object clone() {
			byte[] newRows = new byte[6];
			System.arraycopy(rows, 0, newRows, 0, 6);
			return new Board(newRows);
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			for (int y = 0; y < 6; y++) {
				for (int x = 0; x < 6; x++) {
					builder.append((rows[y] & (1 << x)) != 0 ? 'X' : '.');
				}
				builder.append('\n');
			}
			return builder.toString();
		}
	}
}
