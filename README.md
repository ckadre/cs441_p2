README
Authors: Cameron Kadre, Gail Kinder, Spencer Ford
Class:   cs441
Date:    Nov 2, 2025
Assignment: Project 2

Included Files:
results/ -> contains output files from the benchmark
s12/ -> the source material for S12 pipeline simulation
sim/ -> simulator (main)
test/ -> mem files of our benchmarks from p1
single_cycle_results/ -> contains output files from the p1 benchmark
REAME  
Report -> Documentation 

Compile/Run:
> javac -d out $(find . -name "*.java" -not -path "./out/*")
> java -cp out sim.Sim tests/multiply_space.mem -o mult_space

Most of the documentation can be found in the report. This is our implementation
of S12 pipeline simulator. It is a 5-stage pipeline IF->ID->EX->MEM->WB and it has
latches between every stage. It can implement 5 instructions at a time. It has 
bubbles to stall on mem reads and has WB->EX forwarding. Each clock tick performs
each stage in reverse order so the stages have the most up to date information and
for each tick. We tried to make the design less centeralized in this project because
we knew we needed to keep it organized. 