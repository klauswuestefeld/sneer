package sneer.remote;

import java.io.IOException;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import sneer.life.JpgImage;
import sneer.life.Life;
import sneer.life.LifeView;
import wheel.experiments.Cool;
import wheelexperiments.reactive.Signal;
import wheelexperiments.reactive.Source;
import wheelexperiments.reactive.SourceImpl;
import wheelexperiments.reactive.signals.SetSignal;
import wheelexperiments.reactive.signals.SetSource;

class LifeViewProxy implements LifeView, Serializable {

	transient private final QueryExecuter _queryExecuter;
	transient private boolean _updaterStarted;
	transient private boolean _scoutsSent = false;
	
	private Date _lastSightingDate;

	private IndianForSet<String> _indianForNicknames = new IndianForNicknames();
	private Map<String, LifeView> _contactCache = new HashMap<String, LifeView>();

	private LifeCache _cache;
	
	private final IndianForObject<String> _indianForThoughtOfTheDay = new IndianForThoughtOfTheDay();
	private final IndianForObject<JpgImage> _indianForPicture = new IndianForPicture();

	
	public LifeViewProxy(QueryExecuter queryExecuter) {
		_queryExecuter = queryExecuter;
	}

	private void sendScouts() {
		if (_scoutsSent) return;
		try {
			_queryExecuter.execute(_indianForThoughtOfTheDay);
			_queryExecuter.execute(_indianForPicture);
			_scoutsSent = true;
		} catch (IOException ignored) {
			ignored.printStackTrace();
			//Simply ignore this exception, since the connection will try to reconnect anyway.
		}
	}

	private Runnable updater() {
		return new Runnable() {
			public void run() {
				while (true) {
					update();
//					Cool.sleep(1000 * 60);
//					Cool.sleep(1000 * 4);
					Cool.sleep(100);
				}
			}
		};
	}

	private void update() {
		try {
			sendScouts();

			LifeCache newCache = _queryExecuter.execute(new LifeSightingQuery());
			if (newCache == null) return;
			if (newCache.lastSightingDate() == null) return;
			_cache = newCache;
			_lastSightingDate = new Date();
		} catch (IOException ignored) {
			_scoutsSent = false;
			//Simply ignore this exception, since the connection will try to reconnect anyway.
		}
	}

	public String name() {
		return _cache.name();
	}

	public Signal<String> thoughtOfTheDay() {
		return _indianForThoughtOfTheDay.localSourceToNotify();
	}

	public SetSignal<String> nicknames() {
		return _indianForNicknames.localSetSourceToNotify();
	}
	
	public LifeView contact(String nickname) {
		if (!nicknames().currentValue().contains(nickname)) return null;

		LifeView cached = _contactCache.get(nickname);
		if (cached != null) return cached;
		
		LifeView proxy = new LifeViewProxy(new QueryRouter(nickname, _queryExecuter));
		_contactCache.put(nickname, proxy);
		return proxy;
	}

	public String contactInfo() {
		return _cache.contactInfo();
	}

	public Signal<JpgImage> picture() {
		return _indianForPicture.localSourceToNotify();
	}

	public Object thing(String name) {
		return _cache.thing(name);
	}

	public Map<String, Object> things() {
		return _cache.things();
	}

	public Date lastSightingDate() {
		triggerUpdater();  //LifeViewProxies are recreated when the transactions get replayed. Only the ones that are actually used must start their thread.
        return _lastSightingDate;
	}

	private void triggerUpdater() {
		if (_updaterStarted) return;
		_updaterStarted = true;
		Cool.startDaemon(updater());
	}

	private static final long serialVersionUID = 1L;

	
	static private class IndianForThoughtOfTheDay extends IndianForObject<String> {
		@Override
		protected Signal<String> signalToObserveOn(Life life) {
			return life.thoughtOfTheDay();
		}

		@Override
		protected Source<String> createLocalSourceToNotify() {
			return new SourceImpl<String>();
		}

		private static final long serialVersionUID = 1L;
	}

	static private class IndianForNicknames extends IndianForSet<String> {

		private static final long serialVersionUID = 1L;

		@Override
		protected SetSignal<String> setSignalToObserveOn(Life life) {
			return life.nicknames();
		}

		@Override
		protected SetSource<String> createLocalSetSourceToNotify() {
			return new SetSource<String>();
		}

	}

	static private class IndianForPicture extends IndianForObject<JpgImage> {
		@Override
		protected Signal<JpgImage> signalToObserveOn(Life life) {
			return life.picture();
		}

		@Override
		protected Source<JpgImage> createLocalSourceToNotify() {
			return new SourceImpl<JpgImage>();
		}

		private static final long serialVersionUID = 1L;
	}


}
