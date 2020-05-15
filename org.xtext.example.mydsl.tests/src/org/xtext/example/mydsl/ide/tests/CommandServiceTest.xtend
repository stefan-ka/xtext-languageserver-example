package org.xtext.example.mydsl.ide.tests

import com.google.gson.JsonPrimitive
import org.eclipse.lsp4j.ClientCapabilities
import org.eclipse.lsp4j.ExecuteCommandCapabilities
import org.eclipse.lsp4j.ExecuteCommandParams
import org.eclipse.lsp4j.WorkspaceClientCapabilities
import org.eclipse.xtext.testing.AbstractLanguageServerTest
import org.junit.Test
import org.eclipse.lsp4j.WorkspaceEdit

class CommandServiceTest extends AbstractLanguageServerTest {

	new() {
		super("mydsl")
	}

	/**
	 * This one works, as there is no reference to the Xtext greeting.
	 */
	@Test
	def void testRemoveXtextGreeting() {
		// given
		initialize[
			capabilities = new ClientCapabilities => [
				workspace = new WorkspaceClientCapabilities => [
					executeCommand = new ExecuteCommandCapabilities => [
						dynamicRegistration = true
					]
				]
			]
		]
		val input = '''
			Hello Xtext!
			Hello ThisFile from Other!
			Hello You!
		'''
		val fileURI = 'test.mydsl'.writeFile(input)

		// when
		val result = languageServer.executeCommand(
			new ExecuteCommandParams("mydsl.remove.xtext.greeting", #[new JsonPrimitive(fileURI)]))
		val edit = result.get as WorkspaceEdit

		// then
		assertEquals('''
			changes :
			    test.mydsl : 
			    
			     [[0, 0] .. [1, 0]]
			documentChanges : 
		  '''.toString, toExpectation(edit))
	}

	/**
	 * This one fails in the ReferenceUpdater, due to the reference in the second line of the input.
	 */
	@Test
	def void testRemoveXtextGreetingWithReference() {
		// given
		initialize[
			capabilities = new ClientCapabilities => [
				workspace = new WorkspaceClientCapabilities => [
					executeCommand = new ExecuteCommandCapabilities => [
						dynamicRegistration = true
					]
				]
			]
		]
		val input = '''
			Hello Xtext!
			Hello VSCode from Xtext!
			Hello ThisFile from Other!
			Hello You!
		'''
		val fileURI = 'test.mydsl'.writeFile(input)

		// when
		val result = languageServer.executeCommand(
			new ExecuteCommandParams("mydsl.remove.xtext.greeting", #[new JsonPrimitive(fileURI)]))
		val edit = result.get as WorkspaceEdit

		// then
		assertEquals('''
			changes :
			    test.mydsl : 
			    
			     [[0, 0] .. [2, 0]]
			documentChanges : 
		  '''.toString, toExpectation(edit))
	}

}
