package br.ufmg.dcc.vod.ncrawler.distributed.rmi.client;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.MalformedURLException;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.HashSet;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import br.ufmg.dcc.vod.ncrawler.distributed.rmi.client.ServerID;
import br.ufmg.dcc.vod.ncrawler.distributed.rmi.server.JobExecutor;
import br.ufmg.dcc.vod.ncrawler.distributed.rmi.server.JobExecutorBuilder;

public class ServerIDTest {

	private HashSet<JobExecutorBuilder> serverFactories;

	@Before
	public void setUp() throws Exception {
		serverFactories = new HashSet<JobExecutorBuilder>();
	}

	@After
	public void tearDown() throws Exception {
		for (JobExecutorBuilder f : serverFactories) {
			f.shutdown();
		}
	}
	
	@Test
	public void testAll() 
			throws RemoteException, AlreadyBoundException, 
			MalformedURLException, NotBoundException {
		ServerID sid = new ServerID("localhost", 9090);
		
		JobExecutor resolve;
		try {
			resolve = sid.resolve();
			fail();
		} catch (Exception e) {
		}
		
		JobExecutorBuilder f = new JobExecutorBuilder(9090);
		f.createAndBind();

		resolve = sid.resolve();
		assertTrue(resolve != null);
		
		sid.reset();
		JobExecutor resolve2 = sid.resolve();
		assertTrue(resolve2 != resolve);
		
		f.shutdown();
	}
}