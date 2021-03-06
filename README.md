# cache-me-if-you-can
A program for simulating caches.

## About
This program finds the cache number of cache hits and the number of the different types of cache misses for a cache specified by the user for a trace of address accesses specified by the user. Each Cache uses a Least Recently Used replacement policy.

## Organization of a cache

<img src="https://github.com/nating/cache-me-if-you-can/blob/master/assets/cache-explanation.png" width="800">

## Calculations
This is how each cache-miss 'type' is calculated. The definition of each of these 'type's of cache-miss is given as just the way in which they are calculated, as below.

<img src="https://github.com/nating/cache-me-if-you-can/blob/master/assets/cache-miss-calculations.jpg" width="600">

## Usage

`java Cache <bytes per block> <lines in a set> <sets in the cache> <addresses accessed in trace>`

This command must be followed by x lines of 32 bit addresses for the trace written in hexidecimal without the "0x" prefix. Where x is the number of addresses accessed in the trace.

## Example
Here is an example using the cache from the visualisation above.

### Input
java Cache 16 4 2 32  
0000  
0004  
000c  
2200  
00d0  
00e0  
1130  
0028  
113c  
2204  
0010  
0020   
0004  
0040  
2208  
0008  
00a0  
0004  
1104  
0028  
000c  
0084  
000c  
3390  
00b0  
1100  
0028  
0064  
0070  
00d0  
0008  
3394  

### Output
Total Hits: 15  
Total Misses: 17  
Compulsory: 15  
Capacity: 1  
Conflict: 1  
