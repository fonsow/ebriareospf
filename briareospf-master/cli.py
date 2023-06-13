import argparse
import subprocess

# Define the path to the tracer programs
ENTER_TRACER_PATH = "syscall_enter_tracer.py"
EXIT_TRACER_PATH = "syscall_exit_tracer.py"

examples = """
Examples:
  sudo python3 cli.py enter -p 1234 -s 42
  sudo python3 cli.py exit --pid 5678 --syscall 24
  sudo python3 cli.py enter --bt 5000 --syscall 25
"""

# Create the argument parser
parser = argparse.ArgumentParser(
    prog="CLI",
    description="CLI that can run 2 programs that Trace System calls",
    formatter_class=argparse.RawDescriptionHelpFormatter,
    epilog=examples)

group = parser.add_mutually_exclusive_group()

# Add the tracer choice flag
parser.add_argument("tracer", choices=["enter", "exit"], help="Choose 'enter' or 'exit' tracer")

# Add the PID flag
group.add_argument("-p", "--pid", help="Comma-separated PIDs")

# Add the syscall flag
parser.add_argument("-s", "--syscall", help="Comma-separated system call numbers")

# Add the bt flag (only valid without -p)
group.add_argument("--bt", help="Filter PIDs bigger than X")

# Parse the command line arguments
args = parser.parse_args()

# Determine which tracer program to run based on the user input
if args.tracer == "enter":
    tracer_path = ENTER_TRACER_PATH
else:
    tracer_path = EXIT_TRACER_PATH

# Build the arguments list
cmd_args = []
if args.pid:
    cmd_args += ["--pid", args.pid]
elif args.bt:
    cmd_args += ["--bt", args.bt]

if args.syscall:
    cmd_args += ["--syscall", args.syscall]

# Run the tracer program with the specified arguments (if any)
p = subprocess.Popen(["python3", tracer_path] + cmd_args)
input("Press Enter to stop the tracer program\n")
p.terminate()