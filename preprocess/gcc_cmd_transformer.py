import sys
import os
from pipes import quote


def main(argv):
	argv = argv[1:] # Remove this script's name

	# Extract c-files in the commandline
	files = filter(lambda a: a.endswith(".c"), argv)

	# remove all c-files
	argv = filter(lambda a: not a in files, argv)

	# remove -o and next
	i = 1
	while(i < len(argv)):
		if(argv[i-1] == "-o"):
			argv = argv[0:i-1] + argv[i+1:]
		else:
			i += 1

	# remove -c's
	argv = filter(lambda a: not a.startswith("-c"), argv)

	# remove -l's
	argv = filter(lambda a: not a.startswith("-l"), argv)

	# remove -L's
	argv = filter(lambda a: not a.startswith("-L"), argv)


#-MT -MD -MP -MF
	argv = filter(lambda a: not a.startswith("-MT"), argv)
	argv = filter(lambda a: not a.startswith("-MD"), argv)
	argv = filter(lambda a: not a.startswith("-MP"), argv)
	argv = filter(lambda a: not a.startswith("-MF"), argv)

# mod_flv_streaming.lo .deps/lemon.Tpo
	argv = filter(lambda a: not a.endswith(".lo"), argv)
	argv = filter(lambda a: not a.endswith(".Tpo"), argv)

	# remove object-files? possible?
	argv = filter(lambda a: not a.endswith(".o"), argv)




	other_dir = "/home/user/Desktop/preprocess/preprocessed/"

	# build new gcc line
	for file_name in files:
		# -E  # no include
		# -P  # no linemarkers
		cmd = ["gcc", "-c", "-E", "-P"]
		cmd += argv
		target_file = other_dir + file_name
		cmd += [file_name, "-o", target_file]

		cmd = map(lambda c: quote(c), cmd)

		target_dir = os.path.dirname(os.path.realpath(target_file))
		cmd2 = "mkdir -p " + quote(target_dir)
		print cmd2

		print " ".join(cmd)

if(__name__ == "__main__"):
	main(sys.argv)
