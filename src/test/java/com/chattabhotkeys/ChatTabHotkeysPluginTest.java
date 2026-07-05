package com.chattabhotkeys;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class ChatTabHotkeysPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(ChatTabHotkeysPlugin.class);
		RuneLite.main(args);
	}
}
