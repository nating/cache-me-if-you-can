import java.util.Scanner;

public class Cache {
	
	//L == amount of bytes in each block
	//K == spaces available in a set
	//N == amount of sets
	
	//The tag with LRU bits of zero is the least recently used.
	
	private int L;
	private int K;
	private int N;
	private int size;
	private int hits;
	private int totalMisses;
	private int compulsoryMisses;
	private int capacityMisses;
	private int conflictMisses;

	private final int log2L;
	private final int log2K;
	private final int log2N;
	
	//Binary is stored in the cache as strings of 1s and 0s
	//Each tag has a 'dirty-bit', a 'valid-bit', and 'LRU bit's in the corresponding position in the 'dirty' & 'valid' array.

	private String[][][] bytes; 		// [set][index][offset]
	private String[][] tags;			// [set][index]
	private String[][] LRUBits;			// [set][index]
	private boolean[][] dirty;			// [set][index]
	private boolean[][] valid;			// [set][index]
	
	public Cache(int L, int K, int N){
		this.L = L;
		this.N = N;
		this.K = K;
		this.size = L*N*K;
		
		//Using the log identity 'log[b]x = log[a]x / log[a]b'
		this.log2L = (int) (Math.log(L)/Math.log(2));
		this.log2N = (int) (Math.log(N)/Math.log(2));
		this.log2K = (int) (Math.log(K)/Math.log(2));
		this.conflictMisses = 0;

		this.tags = new String[N][K];
		this.LRUBits = new String[N][K];
		this.dirty = new boolean[N][K];
		this.valid = new boolean[N][K];
		this.bytes = new String[N][K][L];
		
		String lru = "";
		for(int i=0;i<log2K;i++){
			lru+="0";
		}
		
		for(int i=0;i<N;i++){
			for(int j=0;j<K;j++){
				LRUBits[i][j] = lru;
			}
		}
	}
	
	//Returns the bitString at the address
	public String read(String address){
		String addressString = hexToBinary(address,L);
		String offsetString = addressString.substring(addressString.length()-log2L,addressString.length());
		String setString = addressString.substring(addressString.length()-log2L-log2N,addressString.length()-log2L);
		String tagString = addressString.substring(0,addressString.length()-log2L-log2N);
		int set = binaryToInt(setString);
		int offset = binaryToInt(offsetString);
		for(int i=0;i<K;i++){
			String tag = tags[set][i];
			if(tag!=null && tag.equals(tagString) && valid[set][i]){
				System.out.println("HIT");
				hits++;
				
				//Decrement all tags used less recently than the lru of this set
				for(int j=0;j<K;j++){
					if(binaryToInt(LRUBits[set][j])>binaryToInt(LRUBits[set][i])){
						LRUBits[set][j] = decrement(LRUBits[set][j]);
					}
				}

				int bitsInLRU = K==1? 1 : log2K;
				LRUBits[set][i] = intToBinary(K-1,bitsInLRU);
				return bytes[set][i][offset];
			}
		}
		return add(address);
	}
	
	//Returns the bitString at the address
	public String write(String address, String bits){
		String addressString = hexToBinary(address,L);
		String offsetString = addressString.substring(addressString.length()-log2L,addressString.length());
		String setString = addressString.substring(addressString.length()-log2L-log2N,addressString.length()-log2L);
		int set = binaryToInt(setString);
		int offset = binaryToInt(offsetString);
		for(int i=0;i<K;i++){
			if(tags[set][i].equals(addressString) && valid[set][i]){
				hits++;
				dirty[set][i] = true;
				return bytes[set][i][offset] = bits;
			}
		}
		add(address);
		int lru = K;
		int lruIndex = 0;
		for(int i=0;i<K;i++){
			if(binaryToInt(LRUBits[set][i])<lru){
				lru = binaryToInt(LRUBits[set][i]);
				lruIndex = i;
			}
		}
		dirty[set][lruIndex] = true;
		return bytes[set][lruIndex][offset] = bits;
	}
	
	//Returns the bitString at the address added to the cache
	private String add(String address){
		totalMisses++;
		String addressString = hexToBinary(address,L);
		String offsetString = addressString.substring(addressString.length()-log2L,addressString.length());
		String setString = addressString.substring(addressString.length()-log2L-log2N,addressString.length()-log2L);
		String tagString = addressString.substring(0,addressString.length()-log2L-log2N);
		int set = binaryToInt(setString);
		int offset = binaryToInt(offsetString);
		int lru = K;
		int lruIndex = 0;
		for(int i=0;i<K;i++){
			if(binaryToInt(LRUBits[set][i])<lru){
				lru = binaryToInt(LRUBits[set][i]);
				lruIndex = i;
			}
		}
		
		//If old value dirty, then write to memory
		if(dirty[set][lruIndex]){
			writeToMemory(tags[set][lruIndex], bytes[set][lruIndex]);
		}
		
		//Decrement all tags used less recently than the lru of this set
		for(int i=0;i<K;i++){
			if(binaryToInt(LRUBits[set][i])>lru){
				LRUBits[set][i] = decrement(LRUBits[set][i]);
			}
		}
		
		//Update cache line
		tags[set][lruIndex] = tagString;
		int bitsInLRU = K==1? 1 : log2K;
		LRUBits[set][lruIndex] = intToBinary(K-1,bitsInLRU);		//Set lru to be mru
		valid[set][lruIndex] = true;
		dirty[set][lruIndex] = false;
		bytes[set][lruIndex] = getFromMemory(address);
		
		
		return bytes[set][lruIndex][offset];
	}
	
	//Returns the integer that the binary number represents
 	private int binaryToInt(String binary){
		int total = 0;
		int bitSignifigance = 1;
		for(int i=1;i<=binary.length();i++){
			if(binary.charAt(binary.length()-i)=='1'){
				total += bitSignifigance;
			}
			bitSignifigance *= 2;
		}
		return total;
	}
	
	//Returns a string of bits of length 'bits' that represent the integer 'num'
	private String intToBinary(int num, int bits){
		return String.format("%"+bits+"s", Integer.toBinaryString(num)).replace(' ', '0');
	}
	
	//Returns a String of bits of length 'bits' that represents the hex value
	private String hexToBinary(String hex, int bits){
		int num = Integer.parseInt(hex, 16);
		String binary = intToBinary(num,bits);
		return binary;
	}
	
	//Decrements a binary string by 1
	private String decrement(String binary){
		int bin = binaryToInt(binary);
		bin--;
		return intToBinary(bin,binary.length());
	}
	
	//Writes the block in memory indexed by 'address' with the 'bytes'
	private void writeToMemory(String address, String[] bytes){
		
	}
	
	//Returns the block of memory indexed by 'address'
	private String[] getFromMemory(String address){
		String[] theBytes = new String[L];
		return theBytes;
	}
	
	//Prints out the current state of the Cache
	public void print(){
		System.out.println("        lru   tag        d     v");
		for(int set=0;set<N;set++){
			for(int index=0;index<K;index++){
				System.out.println("["+set+"]["+index+"]: "+LRUBits[set][index]+" "+tags[set][index]+" "+dirty[set][index]+" "+valid[set][index]+" ");
				for(int b=0;b<L;b++){
					//System.out.print(bytes[set][index][b]+" ");
				}
			}
			System.out.println("");
		}
	}

	private static void printHitsAndMisses(Cache c){
		System.out.println("Total Hits: "+c.getHits());
		System.out.println("Total Misses: "+c.getTotalMisses());
		System.out.println("Compulsory: "+c.getCompulsoryMisses());
		System.out.println("Capacity: "+c.getCapacityMisses());
		System.out.println("Conflict: "+c.getConflictMisses()+"\n");
	}
	
	public static void main(String[] args){
		
		int l = Integer.parseInt(args[0]);
        int k = Integer.parseInt(args[1]);
        int n = Integer.parseInt(args[2]);
        int x = Integer.parseInt(args[3]);

		Cache c = new Cache(l,k,n);
		Cache fullyAssociativeInfiniteCapacity = new Cache(l,1,600);	//Very large number (600?) that won't be reached
		Cache fullyAssociativeFiniteCapacity = new Cache(l,1,k*n);
		
		Scanner scanner = new Scanner(System.in);
		
		//Simulate the three traces needed to find a cache's miss types.
		for(int i=0;i<x;i++){
			String address = scanner.nextLine();
			c.read(address);
			fullyAssociativeInfiniteCapacity.read(address);
			fullyAssociativeFiniteCapacity.read(address);
		}
		scanner.close();
		
		//Algorithm for finding the different types of misses for a cache.
		c.setCompulsoryMisses(fullyAssociativeInfiniteCapacity.getTotalMisses());
		c.setCapacityMisses(fullyAssociativeFiniteCapacity.getTotalMisses()-fullyAssociativeInfiniteCapacity.getTotalMisses());
		c.setConflictMisses( c.getTotalMisses() - c.getCapacityMisses() - c.getCompulsoryMisses());

		printHitsAndMisses(c);
	}
	

	public int getSize(){
		return size;
	}

	public int getHits() {
		return hits;
	}

	public int getCompulsoryMisses() {
		return compulsoryMisses;
	}

	public int getCapacityMisses() {
		return capacityMisses;
	}

	public int getTotalMisses() {
		return totalMisses;
	}

	public void setCapacityMisses(int c) {
		capacityMisses = c;
	}

	public void setConflictMisses(int c) {
		conflictMisses = c;
	}

	public void setCompulsoryMisses(int c) {
		compulsoryMisses = c;
	}

	public int getConflictMisses() {
		return conflictMisses;
	}
	
}
