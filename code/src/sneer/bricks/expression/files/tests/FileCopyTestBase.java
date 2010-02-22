package sneer.bricks.expression.files.tests;

import static sneer.foundation.environments.Environments.my;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import org.junit.Ignore;
import org.junit.Test;

import sneer.bricks.expression.files.map.mapper.FileMapper;
import sneer.bricks.hardware.cpu.crypto.Sneer1024;
import sneer.bricks.hardware.cpu.lang.contracts.WeakContract;
import sneer.bricks.hardware.io.IO;
import sneer.bricks.pulp.blinkinglights.BlinkingLights;
import sneer.bricks.pulp.blinkinglights.Light;
import sneer.bricks.pulp.reactive.collections.CollectionChange;
import sneer.bricks.software.code.classutils.ClassUtils;
import sneer.bricks.software.folderconfig.tests.BrickTest;
import sneer.foundation.lang.Consumer;

/** Abstract test class names must not end in "Test" or else Hudson will try to instantiate them and fail. :P */

public abstract class FileCopyTestBase extends BrickTest {

	protected final FileMapper _fileMapper = my(FileMapper.class);

	@Ignore
	@Test (timeout = 3000)
	public void testWithZeroLengthFile() throws Exception {
		testWith(zeroLengthFile());
	}
	
	@Test (timeout = 4000)
	public void testWithSmallFile() throws Exception {
		testWith(anySmallFile());
	}

	@Ignore
	@Test (timeout = 6000)
	public void testWithFolder() throws Exception {
		testWith(folderWithAFewFiles());
	}

	@Test (timeout = 7000)
	public void testWithLargeFile() throws Exception {
		testWith(createLargeFile());
	}

	private File createLargeFile() throws IOException {
		File result = newTmpFile();
		my(IO.class).files().writeByteArrayToFile(result, randomBytes(1000000));
		return result;
	}
	
	private byte[] randomBytes(int size) {
		byte[] result = new byte[size];
		new Random().nextBytes(result);
		return result;
	}

	private void testWith(File fileOrFolder) throws Exception {
		@SuppressWarnings("unused")	WeakContract refToAvoidGc =
			my(BlinkingLights.class).lights().addReceiver(new Consumer<CollectionChange<Light>>(){@Override public void consume(CollectionChange<Light> deltas) {
				if (!deltas.elementsAdded().isEmpty())
					fail();
			}});

		File copy = newTmpFile();
		Sneer1024 hash = null;
		if (fileOrFolder.isDirectory()) {
			hash = _fileMapper.mapFolder(fileOrFolder);
			copyFolderFromFileMap(hash, copy);
		} else {
			hash = _fileMapper.mapFile(fileOrFolder);
			copyFileFromFileMap(hash, copy);
		}
		assertNotNull(hash);

		assertSameContents(fileOrFolder, copy);
	}

	abstract protected void copyFileFromFileMap(Sneer1024 hashOfContents, File destination) throws Exception;

	abstract protected void copyFolderFromFileMap(Sneer1024 hashOfContents, File destination) throws Exception;

	private File zeroLengthFile() throws IOException {
		return createTmpFile("tmp" + System.nanoTime());
	}

	private File anySmallFile() {
		return myClassFile();
	}

	private File myClassFile() {
		return my(ClassUtils.class).classFile(getClass());
	}

	private File folderWithAFewFiles() {
		final File result = new File(myClassFile().getParent(), "fixtures");

		final long now = System.currentTimeMillis();
		setLastModifiedRecursively(new File(result, "directory1"), now);
		setLastModifiedRecursively(new File(result, "directory1copy"), now);

		return result;
	}

	private void setLastModifiedRecursively(File file, long lastModified) {
		if (file.isDirectory())
			for (File child : file.listFiles())
				setLastModifiedRecursively(child, lastModified);

		file.setLastModified(lastModified);
	}

	private void assertSameContents(File file1, File file2) throws IOException {
		my(IO.class).files().assertSameContents(file1, file2);
	}

}