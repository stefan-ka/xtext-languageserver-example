package org.xtext.example.mydsl.ide

import com.google.gson.JsonPrimitive
import com.google.inject.Inject
import org.eclipse.lsp4j.ExecuteCommandParams
import org.eclipse.xtext.ide.server.ILanguageServerAccess
import org.eclipse.xtext.ide.server.commands.IExecutableCommandService
import org.eclipse.xtext.util.CancelIndicator
import org.xtext.example.mydsl.ide.commands.SampleCommandExecutor

class CommandService implements IExecutableCommandService {

	@Inject
	private SampleCommandExecutor commandExecutor;

	override initialize() {
		#["mydsl.a", "mydsl.b", "mydsl.c", "mydsl.remove.xtext.greeting"]
	}

	override execute(ExecuteCommandParams params, ILanguageServerAccess access, CancelIndicator cancelIndicator) {
		if (params.command == "mydsl.a") {
			val uri = params.arguments.head as String
			if (uri !== null) {
				return access.doRead(uri) [
					return "Command A"
				].get
			} else {
				return "Param Uri Missing"
			}
		} else if (params.command == "mydsl.b") {
			return "Command B"
		} else if (params.command == "mydsl.remove.xtext.greeting") {
			val uri = params.arguments.head as JsonPrimitive
			return access.doRead(uri.asString) [
				try {
					return commandExecutor.executeCommand(resource, document, access, params)
				} catch (Exception e) {
					e.printStackTrace
					return "Error at executing command: " + e.message;
				}
			].get
		} else {
			return "Bad Command"
		}

	}

}
