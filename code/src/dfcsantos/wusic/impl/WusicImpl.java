package dfcsantos.wusic.impl;

import static sneer.foundation.environments.Environments.my;

import java.io.File;

import sneer.bricks.hardware.cpu.lang.contracts.WeakContract;
import sneer.bricks.pulp.reactive.Register;
import sneer.bricks.pulp.reactive.Signal;
import sneer.bricks.pulp.reactive.Signals;
import sneer.foundation.lang.Closure;
import sneer.foundation.lang.Consumer;
import sneer.foundation.lang.PickyConsumer;
import sneer.foundation.lang.exceptions.Refusal;
import dfcsantos.tracks.Track;
import dfcsantos.tracks.sharing.endorsements.client.TrackClient;
import dfcsantos.tracks.sharing.endorsements.client.downloads.counter.TrackDownloadCounter;
import dfcsantos.tracks.storage.folder.TracksFolderKeeper;
import dfcsantos.wusic.Wusic;

public class WusicImpl implements Wusic {

	private final Register<OperatingMode> _currentOperatingMode = my(Signals.class).newRegister(OperatingMode.OWN);

	private TrackSourceStrategy _trackSource = OwnTracks.INSTANCE;
	private final Register<Track> _trackToPlay = my(Signals.class).newRegister(null);
	private Track _lastPlayedTrack;

	private final DJ _dj = new DJ(_trackToPlay.output(), new Closure() { @Override public void run() { skip(); } } );

	private Register<Boolean> _isTracksDownloadActive = my(Signals.class).newRegister(false);
	private final Register<Integer> _tracksDownloadAllowance = my(Signals.class).newRegister(DEFAULT_TRACKS_DOWNLOAD_ALLOWANCE);  

	@SuppressWarnings("unused") private final WeakContract _downloadAllowanceConsumerContract;
	@SuppressWarnings("unused") private final WeakContract _isDownloadEnabledConsumerContract;

	@SuppressWarnings("unused") private final WeakContract _operatingModeConsumerContract;

	WusicImpl() {
		restore();

		my(TrackClient.class).setOnOffSwitch(isTracksDownloadActive());

		_isDownloadEnabledConsumerContract = isTracksDownloadActive().addReceiver(new Consumer<Boolean>() { @Override public void consume(Boolean isDownloadAllowed) {
			save();
		}});

		_downloadAllowanceConsumerContract = tracksDownloadAllowance().addReceiver(new Consumer<Integer>(){ @Override public void consume(Integer downloadAllowance) {
			save();
		}});

		_operatingModeConsumerContract = operatingMode().addReceiver(new Consumer<OperatingMode>() { @Override public void consume(OperatingMode mode) {
			reset();
			_trackSource = (mode.equals(OperatingMode.OWN)) ? OwnTracks.INSTANCE : PeerTracks.INSTANCE;
		}});
	}

	private void restore() {
		Object[] restoredDownloadAllowanceState = Store.restore();
		if (restoredDownloadAllowanceState == null) return;

		_isTracksDownloadActive.setter().consume((Boolean) restoredDownloadAllowanceState[0]);
		try {
			tracksDownloadAllowanceSetter().consume((Integer) restoredDownloadAllowanceState[1]);
		} catch (Refusal e) {
			throw new IllegalStateException(e);
		}
	}

	private void save() {
		Store.save(isTracksDownloadActive().currentValue(), tracksDownloadAllowance().currentValue());
	}

	private void reset() {
		stop();
		_lastPlayedTrack = null;
	}

	@Override
	public void switchOperatingMode() {
		setOperatingMode(operatingMode().currentValue().equals(OperatingMode.OWN) ? OperatingMode.PEERS : OperatingMode.OWN);
	}

	private void setOperatingMode(OperatingMode mode) {
		_currentOperatingMode.setter().consume(mode);
	}

	@Override
	public Signal<OperatingMode> operatingMode() {
		return _currentOperatingMode.output();
	}

	@Override
	public void setPlayingFolder(File playingFolder) {
		my(TracksFolderKeeper.class).setPlayingFolder(playingFolder);
		skip();
	}

	@Override
	public void setSharedTracksFolder(File sharedTracksFolder) {
		my(TracksFolderKeeper.class).setSharedTracksFolder(sharedTracksFolder);
	}

	@Override
	public void setShuffle(boolean shuffle) {
		((OwnTracks)_trackSource).setShuffle(shuffle);
	}

	@Override
	public void start() {
		skip();
	}

	@Override
	public void pauseResume() {
		if (currentTrack() == null)
			play();
		else
			_dj.pauseResume();
	}

	private Track currentTrack() {
		return _trackToPlay.output().currentValue();
	}

	@Override
	public void back() {
		throw new sneer.foundation.lang.exceptions.NotImplementedYet(); // Implement
	}

	@Override
	public void skip() {
		Track nextTrack = _trackSource.nextTrack();
		if (nextTrack == null || nextTrack.equals(_lastPlayedTrack))
			stop();
		play(nextTrack);
	}

	private void play() {
		if (_lastPlayedTrack == null)
			skip();
		else
			play(_lastPlayedTrack);
	}

	private void play(final Track track) {
		_trackToPlay.setter().consume(track);
		if (track != null) _lastPlayedTrack = track;
	}

	@Override
	public void stop() {
		play(null);
	}

	@Override
	public void meToo() {
		((PeerTracks)_trackSource).meToo(_trackToPlay.output().currentValue());
	}

	@Override
	public void deleteTrack() {
		final Track currentTrack = currentTrack();
		if (currentTrack == null) return;
		
		skip();
		_trackSource.deleteTrack(currentTrack);
	}

	@Override
	public Signal<Boolean> isPlaying() {
		return _dj.isPlaying();
	}

	@Override
	public Signal<Track> playingTrack() {
		return _trackToPlay.output();
	}

	@Override
	public Signal<Integer> playingTrackTime() {
		return _dj.trackElapsedTime();
	}

	@Override
	public Signal<Integer> numberOfPeerTracks() {
		return my(TrackDownloadCounter.class).count();
	}

	@Override
	public Signal<Boolean> isTracksDownloadActive() {
		return _isTracksDownloadActive.output();
	}

	@Override
	public Consumer<Boolean> tracksDownloadActivator() {
		return _isTracksDownloadActive.setter();
	}

	@Override
	public Signal<Integer> tracksDownloadAllowance() {
		return _tracksDownloadAllowance.output();
	}

	@Override
	public PickyConsumer<Integer> tracksDownloadAllowanceSetter() {
		return new PickyConsumer<Integer>() { @Override public void consume(Integer allowanceInMBs) throws Refusal {
			validateDownloadAllowance(allowanceInMBs);
			_tracksDownloadAllowance.setter().consume(allowanceInMBs);
		}};
	}

	private void validateDownloadAllowance(Integer allowanceInMBs) throws Refusal {
		if (allowanceInMBs == null || allowanceInMBs < 0) throw new Refusal("Invalid tracks' download allowance: it must be positive integer");
	}

}


