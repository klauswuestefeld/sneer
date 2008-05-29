package sneer.bricks.brickmanager.impl;

import java.io.File;
import java.io.IOException;
import java.util.List;

import sneer.bricks.brickmanager.BrickManager;
import sneer.bricks.brickmanager.BrickManagerException;
import sneer.bricks.config.SneerConfig;
import sneer.bricks.dependency.Dependency;
import sneer.bricks.dependency.DependencyManager;
import sneer.bricks.deployer.BrickBundle;
import sneer.bricks.deployer.BrickFile;
import sneer.bricks.keymanager.KeyManager;
import sneer.bricks.keymanager.PublicKey;
import sneer.bricks.log.Logger;
import sneer.bricks.mesh.Party;
import sneer.lego.Container;
import sneer.lego.Inject;
import sneer.lego.utils.InjectedBrick;
import wheel.lang.exceptions.NotImplementedYet;
import wheel.reactive.maps.MapRegister;
import wheel.reactive.maps.MapSignal;
import wheel.reactive.maps.impl.MapRegisterImpl;


public class BrickManagerImpl implements BrickManager {

	@Inject
	private SneerConfig _config;

	@Inject
	private Logger _log;
	
	@Inject
	private DependencyManager _dependencyManager;
	
	@Inject
	private KeyManager _keyManager;
	
	@Inject
	private Container _container;
	
	private MapRegister<String, BrickFile> _bricksByName = new MapRegisterImpl<String, BrickFile>();

	@Override
	public void install(BrickBundle bundle) {
		
		/*
		 * Must install brick on the right order, otherwise the runOnceOnInstall 
		 * will fail because the brick dependencies will not be found on the filesystem.
		 * sorting will fail if dependency cycles are found
		 */
		bundle.sort(); 
		
		List<String> brickNames = bundle.brickNames();
		for (String brickName : brickNames) {
			BrickFile brick = bundle.brick(brickName);
			if(okToInstall(brick)) {
				resolve(bundle, brick);
				install(brick);
			} else {
				//what should we do?
				throw new BrickManagerException("brick: "+brickName+" could not be installed");
			}
		}
	}

	private void resolve(BrickFile brick) {
		resolve(null, brick);
	}
	
	private void resolve(BrickBundle bundle, BrickFile brick) {
		List<InjectedBrick> injectedBricks;
		try {
			injectedBricks = brick.injectedBricks();
		} catch (IOException e) {
			throw new BrickManagerException("Error searching for injected bricks on "+brick.name(), e);
		}
		for (InjectedBrick injected : injectedBricks) {
			String wanted = injected.brickName();
			BrickFile inBundle = bundle == null ? null : bundle.brick(wanted); 
			if(inBundle == null) { 
				//not inBudle, try local registry
				inBundle = _bricksByName.get(wanted);
				if(inBundle == null) {
					//not found. must ask other peer via network
					BrickFile justGotten = retrieveRemoveBrick(brick.origin(), injected.brickName());
					install(justGotten);
				}
			}
		}
		brick.resolved(true);
	}

	private BrickFile retrieveRemoveBrick(PublicKey origin, String brickName) {
		Party party = _keyManager.partyGiven(origin);
		MapSignal<String, BrickFile> remoteBricks = party.mapSignal("bricks");
		BrickFile remoteBrick = remoteBricks.currentGet(brickName);
		
		if(remoteBrick == null)
			throw new NotImplementedYet("Remote brick not found on remote signal");
		
		return remoteBrick;
	}

	@Override
	public BrickFile brick(String brickName) {
		return _bricksByName.get(brickName);
	}

	private boolean okToInstall(BrickFile brick) {
		String brickName = brick.name();
		BrickFile installed = _bricksByName.get(brickName);
		if(installed == null)
			return true;
		
		//compare hashes
		throw new wheel.lang.exceptions.NotImplementedYet(); // Implement
	}

	@Override
	public void install(BrickFile brick) throws BrickManagerException {
		String brickName = brick.name();
		_log.debug("Installing brick: "+brickName);
		
		//0. resolve injected Bricks
		if(!brick.resolved())
			resolve(brick);

		//1. create brick directory under sneer home
		File brickDirectory = brickDirectory(brickName);
		//System.out.println("installing "+brickName+" on "+brickDirectory);
		
		if(brickDirectory.exists()) 
			sneer.lego.utils.FileUtils.cleanDirectory(brickDirectory); //FixUrgent: ask permission to overwrite?
		else 
			brickDirectory.mkdir();
		
		//2. copy received files
		BrickFile installed = copyBrickFiles(brick, brickDirectory);
		
		//3. install dependencies
		copyDependencies(brick, installed);

		//4. update origin. When origin is null we are installing the brick locally, not using meToo
		PublicKey origin = brick.origin();
		origin = origin != null ? origin : _keyManager.ownPublicKey(); 
		installed.origin(origin);

		//5. give the brick a chance to initialize itself (register menus, etc)
		runOnceOnInstall(installed);
		
		_bricksByName.put(brickName, installed);
	}

	private void runOnceOnInstall(BrickFile installed) {
		String brickName = installed.name();
		//System.out.println("RunOnce: "+brickName);
		_container.produce(brickName);
	}

	private void copyDependencies(BrickFile brick, BrickFile installed) {
		String brickName = brick.name();
		List<Dependency> brickDependencies = brick.dependencies();
		for (Dependency dependency : brickDependencies) {
			try {
				dependency = _dependencyManager.add(brickName, dependency);
				installed.dependencies().add(dependency);
			} catch (IOException e) {
				throw new BrickManagerException("Error installing dependecy: "+dependency, e);
			}
		}
	}

	private BrickFile copyBrickFiles(BrickFile brick, File brickDirectory) {
		BrickFile installed;
		try {
			installed = brick.copyTo(brickDirectory);
		} catch (IOException e) {
			throw new BrickManagerException("Error copying brick files to: "+brickDirectory);
		}
		return installed;
	}
	
	private File brickDirectory(String brickName) {
		File root = _config.brickRootDirectory();
		File brickDirectory = new File(root, brickName);
		return brickDirectory;
	}

	@Override
	public MapSignal<String, BrickFile> bricks() {
		return _bricksByName.output();
	}
}
