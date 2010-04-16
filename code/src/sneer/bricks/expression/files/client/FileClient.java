package sneer.bricks.expression.files.client;

import java.io.File;

import sneer.bricks.expression.files.client.downloads.Download;
import sneer.bricks.hardware.cpu.crypto.Hash;
import sneer.bricks.network.social.contacts.Contact;
import sneer.foundation.brickness.Brick;

@Brick
public interface FileClient {

	Download startFileDownload(File file, long lastModified, Hash hashOfFile, Contact source);
	Download startFileDownload(File file, Hash hashOfFile);

	Download startFolderDownload(File folder, long lastModified, Hash hashOfFolder);
	Download startFolderDownload(File folder, Hash hashOfFolder);

}

