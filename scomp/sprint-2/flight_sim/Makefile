CC=gcc
CFLAGS=-Wall -Wextra -g -std=c99
LDLIBS=-lm

OBJS=flight_sim.o flight_proc.o violation.o report_proc.o config.o

flight_sim: $(OBJS)
	$(CC) $(CFLAGS) $(OBJS) -o flight_sim $(LDLIBS)

flight_sim.o: flight_sim.c types.h
flight_proc.o: flight_proc.c types.h
violation.o: violation.c types.h
report_proc.o: report_proc.c types.h
config.o: config.c types.h

clean:
	rm -f $(OBJS) flight_sim report.txt violations.log

run-clean: flight_sim
	./flight_sim clean

run-violation: flight_sim
	./flight_sim violation

run-threshold: flight_sim
	./flight_sim threshold
