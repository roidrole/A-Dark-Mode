/*
	File for automatic update of the pack
	Has been confirmed to work with Java 21.
	To execute, simply call "java NewRelease.java"
	A --version flag can be added to skip the version prompt

	Requires oxipng to be installed on the system for lossless compression of pngs.

	Uses the file in "./Release Ready".
	If mods are added, the format **must** be a line starting with "Added support for:"
	followed by a series of lines composed of "- ", then by the mod name
	It does not check for duplicates, so added support for AE2UEL will show as a separate mod to AE2.
*/
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Stream;

public class NewRelease {

	static String version;

	public static void main(String[] args) throws IOException, InterruptedException {
		if (args.length == 2) {
			if (!Objects.equals(args[0], "--version")) {
				throw new UnsupportedOperationException("Only accepted CLI option is --version");
			}
			version = args[1];
		} else {
			System.out.print("Version number: ");
			version = new Scanner(System.in).nextLine();
		}

		//Update readme — mod list, version
		handleReadme();

		//Compress pngs
		System.out.println("Compressing pngs...");
		execCommand("oxipng", "-r", "./Release Ready/");

		//Copy files over
		System.out.println("Merging Release Ready with Resource Pack/assets");
		Path releaseReadyPath = Path.of("./Release Ready/");
		try(Stream<Path> stream = Files.walk(releaseReadyPath)){stream
			.filter(path -> path.toFile().isFile())
			.filter(path -> path.toString().endsWith(".png"))
			.forEach(path -> {
				Path out = Path.of("./Resource Pack/assets/").resolve(path.subpath(2, path.getNameCount()));
				if(!out.toFile().mkdirs()){
					System.out.println("ERROR: Can't create folders for file "+path);
					return;
				}
				try {
					Files.move(path, out, StandardCopyOption.REPLACE_EXISTING);
				} catch (IOException e) {
					System.out.println("ERROR: Can't transfer file "+path+" from release ready to resource pack");
					e.printStackTrace();
				}
			});
		}
		//Remove orphan directories
		try(Stream<Path> stream = Files.walk(releaseReadyPath)) {stream
			.filter(path -> !path.toFile().isFile())
			.filter(path -> path.getNameCount() > 2)
			.sorted(Comparator.comparingInt(Path::getNameCount).reversed())
			.forEachOrdered(path -> {
				if(!path.toFile().delete()){
					System.out.println("ERROR: Couldn't delete file "+path+" this is likely due to it still having files.");
				}
			});
		}

		builder.directory(new File("./Resource Pack"));
		System.out.println("Creating release zip");
		execCommand("zip", "-q", "-r", "Dark Mode-" + version + ".zip", "assets", "pack.mcmeta", "pack.png");
		execCommand("mv", "-f", "./Dark Mode-" + version + ".zip", "../Releases");
		System.out.println("Release zip created");


		//Write changelog
		System.out.println("Updating changelog");
		InputStream changelogOldFile = new FileInputStream("./Changelog.md");
		byte[] changelogOld = changelogOldFile.readAllBytes();
		changelogOldFile.close();
		try (
			OutputStream changelogOut = new FileOutputStream("./Changelog.md");
			InputStream changelogIn = new FileInputStream("./Release Ready/Changelog.md")
		) {
			changelogOut.write(version.getBytes());
			changelogOut.write(':');
			changelogOut.write('\n');
			changelogIn.transferTo(changelogOut);
			changelogOut.write('\n');
			changelogOut.write('\n');
			changelogOut.write(changelogOld);
			//Wipe Changelog.md
			new FileOutputStream("./Release Ready/Changelog.md").close();
		}
	}

	static void handleReadme() throws IOException {
		//Update version number
		System.out.println("Updating version number...");
		if (version.length() == 5) {
			try (RandomAccessFile file = new RandomAccessFile("./README.md", "rw")) {
				//Hard-coded position of the version number
				file.seek(0x201);
				file.write(version.getBytes());
			}
		} else {
			System.out.print("ERROR: Length of the version string is not 5. You will have to update it manually.");
		}

		//Read mods from changelog
		System.out.println("Reading mods from changelog...");
		List<String> changelogMods = new ArrayList<>();
		try (BufferedReader reader = new BufferedReader(new FileReader("./Release Ready/Changelog.md"))) {
			String line = "";
			while (!(line.startsWith("Added support for"))) {
				line = reader.readLine();
				if (line == null) {
					//If this is null, it means that no lines start with "Added support for"
					//This means we don't need to update the readme
					return;
				}
			}
			line = reader.readLine();
			while (line != null && line.startsWith("- ")) {
				changelogMods.add("1. " + line.substring(2));
				line = reader.readLine();
			}
		}

		//Read README
		System.out.println("Reading README...");
		List<List<String>> sections = new ArrayList<>(3);
		try (BufferedReader reader = new BufferedReader(new FileReader("./README.md"))) {
			String line = "";

			List<String> header = new ArrayList<>();
			while (!(line.startsWith("Supported mods"))) {
				line = reader.readLine();
				header.add(line);
			}
			sections.add(header);

			List<String> modList = new ArrayList<>();
			line = reader.readLine();
			while (line.startsWith("1. ")) {
				modList.add(line);
				line = reader.readLine();
			}
			sections.add(modList);

			List<String> end = new ArrayList<>();
			while (line != null) {
				end.add(line);
				line = reader.readLine();
			}
			end.add(line);
			sections.add(end);
		}

		//Merge changelog mods into readme mods
		changelogMods.sort(Comparator.naturalOrder());
		sections.set(1, mergeSortedLists(sections.get(1), changelogMods, Comparator.naturalOrder()));

		//Write the new readme
		System.out.println("Writing new README...");
		try (PrintWriter writer = new PrintWriter("./README.md")) {
			sections.forEach(section -> {
				section.forEach(s -> {
					if (s == null) {
						return;
					}
					writer.write(s);
					writer.write('\n');
				});
			});
		}
	}

	static ProcessBuilder builder = new ProcessBuilder("").inheritIO();

	static void execCommand(String... command) throws IOException, InterruptedException {
		builder.command(command);
		builder.start().waitFor();
	}

	static <T> List<T> mergeSortedLists(List<T> list1, List<T> list2, Comparator<T> comparator) {
		int index1 = 0;
		int index2 = 0;
		List<T> out = new ArrayList<>(list1.size() + list2.size());
		while (index1 < list1.size() && index2 < list2.size()) {
			if (comparator.compare(list1.get(index1), list2.get(index2)) < 0) {
				out.add(list1.get(index1));
				index1++;
			} else {
				out.add(list2.get(index2));
				index2++;
			}
		}
		while (index1 < list1.size()) {
			out.add(list1.get(index1++));
		}
		while (index2 < list2.size()) {
			out.add(list2.get(index2++));
		}
		return out;
	}
}