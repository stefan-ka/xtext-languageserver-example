package org.xtext.example.mydsl.ide.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.lsp4j.ApplyWorkspaceEditParams;
import org.eclipse.lsp4j.ExecuteCommandParams;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.xtext.formatting2.regionaccess.ITextReplacement;
import org.eclipse.xtext.ide.serializer.IChangeSerializer;
import org.eclipse.xtext.ide.serializer.IEmfResourceChange;
import org.eclipse.xtext.ide.serializer.ITextDocumentChange;
import org.eclipse.xtext.ide.server.Document;
import org.eclipse.xtext.ide.server.ILanguageServerAccess;
import org.eclipse.xtext.util.CollectionBasedAcceptor;
import org.eclipse.xtext.xbase.lib.ListExtensions;
import org.xtext.example.mydsl.myDsl.Greeting;
import org.xtext.example.mydsl.myDsl.Model;

import com.google.common.collect.Iterables;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class SampleCommandExecutor {

	private Provider<IChangeSerializer> serializerProvider;

	@Inject
	public SampleCommandExecutor(Provider<IChangeSerializer> serializerProvider) {
		this.serializerProvider = serializerProvider;
	}

	/**
	 * see demo file: a.mydsl
	 * 
	 * This command shall remove the greeting "Hello Xtext!". Since the greeting is
	 * references (second line of demo file), the reference has to be removed as
	 * well.
	 * 
	 * Finally leads to the problem in the ReferenceUpdater since he searches for
	 * the deleted reference.
	 * 
	 */
	public WorkspaceEdit executeCommand(Resource resource, Document document, ILanguageServerAccess access,
			ExecuteCommandParams params) {
		WorkspaceEdit edit = createWorkspaceEdit(access, resource.getURI(), document, (Resource copiedResource) -> {
			Model model = (Model) copiedResource.getAllContents().next();
			Optional<Greeting> greetingToRemove = model.getGreetings().stream().filter(g -> g.getName().equals("Xtext"))
					.findFirst();
			if (greetingToRemove.isPresent()) {
				// remove all references to "Xtext"
				List<Greeting> greetingReferences = model.getGreetings().stream()
						.filter(g -> g.getFrom().equals("Xtext")).collect(Collectors.toList());
				for (Greeting greeting : greetingReferences) {
					greeting.getFrom().remove(greetingToRemove.get());
				}

				// remove greeting "Xtext" itself
				EcoreUtil.delete(greetingToRemove.get());
			}

		});
		access.getLanguageClient().applyEdit(new ApplyWorkspaceEditParams(edit, "Remove Xtext! Greeting"));
		return edit;
	}

	private WorkspaceEdit createWorkspaceEdit(ILanguageServerAccess access, URI resourceURI, Document document,
			IChangeSerializer.IModification<Resource> mod) {
		ResourceSet rs = access.newLiveScopeResourceSet(resourceURI);
		Resource copy = rs.getResource(resourceURI, true);
		IChangeSerializer serializer = serializerProvider.get();
		EcoreUtil.resolveAll(copy);
		serializer.addModification(copy, mod);
		List<IEmfResourceChange> documentchanges = new ArrayList<>();
		serializer.applyModifications(CollectionBasedAcceptor.of(documentchanges));
		WorkspaceEdit workspaceEdit = new WorkspaceEdit();
		for (ITextDocumentChange documentchange : Iterables.filter(documentchanges, ITextDocumentChange.class)) {
			List<TextEdit> edits = ListExtensions.map(documentchange.getReplacements(),
					(ITextReplacement replacement) -> {
						TextEdit textEdit = new TextEdit();
						textEdit.setNewText(replacement.getReplacementText());
						textEdit.setRange(new Range(document.getPosition(replacement.getOffset()),
								document.getPosition(replacement.getEndOffset())));
						return textEdit;
					});
			workspaceEdit.getChanges().put(documentchange.getNewURI().toString(), edits);
		}
		return workspaceEdit;
	}

}
