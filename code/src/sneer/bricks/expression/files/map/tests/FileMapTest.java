package sneer.bricks.expression.files.map.tests;

import static basis.environments.Environments.my;

import org.junit.Test;

import sneer.bricks.expression.files.map.FileMap;
import sneer.bricks.expression.files.protocol.FileOrFolder;
import sneer.bricks.expression.files.protocol.FolderContents;
import sneer.bricks.hardware.cpu.crypto.Crypto;
import sneer.bricks.hardware.cpu.crypto.Hash;
import sneer.bricks.software.folderconfig.testsupport.BrickTestBase;

public class FileMapTest extends BrickTestBase {

	private final FileMap _subject = my(FileMap.class);
	
	@Test
	public void fileMapping() {
		Hash hash = hash(42);
		
		_subject.putFile("hello.txt", 10234, 1234, hash);
		assertEquals("hello.txt", _subject.getFiles(hash).get(0));
		assertEquals(hash, _subject.getHash("hello.txt"));
		assertEquals(1234, _subject.getLastModified("hello.txt"));

		_subject.remove("hello.txt");
		assertEquals(0, _subject.getFiles(hash).size());
		assertNull(_subject.getHash("hello.txt"));
	}

	
	@Test
	public void rename() {
		populateSubject();
		
		_subject.rename("1/1", "1/1b");
		
		assertEquals(hash( 11), _subject.getHash("1/1b"));
		assertEquals(hash(117), _subject.getHash("1/1b/7"));
		assertEquals(hash(118), _subject.getHash("1/1b/8"));
		assertEquals(hash(121), _subject.getHash("1/2/1"));
		assertEquals(hash(131), _subject.getHash("1/3/1"));
		assertEquals(117, _subject.getLastModified("1/1b/7"));
		assertEquals(118, _subject.getLastModified("1/1b/8"));
		assertEquals(121, _subject.getLastModified("1/2/1"));
		assertEquals(131, _subject.getLastModified("1/3/1"));
		
		assertFalse(_subject.getFolders(hash(119)).isEmpty());
		assertEquals(hash(119), _subject.getHash("AnotherFolderWithHash119"));
	}

	
	@Test
	public void remove() {
		populateSubject();
		
		assertNotNull(_subject.getHash("1/1"));
		assertNotNull(_subject.getHash("1/1/7"));
		assertNotNull(_subject.getHash("1/1/8"));

		_subject.remove("1/1");
		
		assertNull(_subject.getHash("1/1"));
		assertNull(_subject.getHash("1/1/7"));
		assertNull(_subject.getHash("1/1/8"));

		assertEquals("AnotherFolderWithHash119", _subject.getFolders(hash(119)).get(0));
		assertEquals(hash(119), _subject.getHash("AnotherFolderWithHash119"));
	}

	
	@Test
	public void getFolderContents() {
		populateSubject();
		
		FolderContents folder = _subject.getFolderContents(hash(1));
		assertContents(folder.contents,
			new FileOrFolder("1", hash(11))
		);
		
		folder = _subject.getFolderContents(hash(11));
		assertContents(folder.contents,
			new FileOrFolder("7", 1017, 117, hash(117)),
			new FileOrFolder("8", 1018, 118, hash(118)),
			new FileOrFolder("9",      hash(119))
		);
		
	}

	
	@Test
	public void twoPathsWithSameHash() {
		_subject.putFolder("1a", hash(1));
		_subject.putFolder("1b", hash(1));
		_subject.remove("1b");
		assertEquals("1a", _subject.getFolders(hash(1)).get(0));
	}
	
	@Test
	public void fileAndFolderWithSameHash() {
		_subject.putFolder("emptyFolder", hash(1));
		_subject.putFile("emptyFile", 1042, 42, hash(1));
		assertEquals("emptyFile", _subject.getFiles(hash(1)).get(0));
		assertEquals("emptyFolder", _subject.getFolders(hash(1)).get(0));
	}


	private void populateSubject() {
		_subject.putFile(  "1/1/7", 1017, 117, hash(117));
		_subject.putFile(  "1/1/8", 1018, 118, hash(118));
		_subject.putFolder("1/1/9",      hash(119));
		_subject.putFolder("1/1"  ,      hash( 11));
		_subject.putFolder("1"    ,      hash(  1));
		_subject.putFile(  "1/2/1", 1021, 121, hash(121));
		_subject.putFile(  "1/3/1", 1031, 131, hash(131));
		
		_subject.putFolder("AnotherFolderWithHash119", hash(119));
	}


	private Hash hash(int b) {
		return my(Crypto.class).digest(new byte[] { (byte) b });
	}

}
