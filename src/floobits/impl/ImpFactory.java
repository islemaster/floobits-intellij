package floobits.impl;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import floobits.common.EditorScheduler;
import floobits.common.interfaces.IDoc;
import floobits.common.interfaces.IFactory;
import floobits.common.interfaces.IFile;
import floobits.utilities.Flog;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class ImpFactory implements IFactory {

    private final ImpContext context;
    private final EditorScheduler editor;
    private final LocalFileSystem instance = LocalFileSystem.getInstance();

    public ImpFactory(ImpContext context, EditorScheduler editor) {
        this.context = context;
        this.editor = editor;
    }

    private VirtualFile getVirtualFile(String relPath) {
        VirtualFile fileByPath = instance.findFileByPath(context.absPath(relPath));
        if (fileByPath != null && fileByPath.isValid()) {
            return fileByPath;
        }
        return null;
    }

    public IDoc makeVFile(ImpFile vFile) {
        Document document = FileDocumentManager.getInstance().getDocument(vFile.virtualFile);
        if (document == null) {
            return null;
        }
        return new ImpDoc(context, document);
    }

    @Override
    public void clearReadOnlyState() {
        for (String path : readOnlyBufferIds) {
            VirtualFile file = getVirtualFile(path);
            if (file == null) {
                continue;
            }
            Document document = FileDocumentManager.getInstance().getDocument(file);
            if (document == null) {
                continue;
            }
            document.setReadOnly(false);
        }
        readOnlyBufferIds.clear();
    }

    @Override
    public void removeHighlightsForUser(int userID) {
        HashMap<String, LinkedList<RangeHighlighter>> integerRangeHighlighterHashMap = ImpDoc.highlights.get(userID);
        if (integerRangeHighlighterHashMap == null) {
            return;
        }

        for (Map.Entry<String, LinkedList<RangeHighlighter>> entry : integerRangeHighlighterHashMap.entrySet()) {
            removeHighlight(userID, entry.getKey());
        }
    }

    @Override
    public boolean openFile(File file) {
        VirtualFile floorc = LocalFileSystem.getInstance().findFileByIoFile(file);
        if (floorc == null) {
            return false;
        }
        FileEditorManager.getInstance(context.project).openFile(floorc, true);
        return true;
    }

    @Override
    public void removeHighlight(Integer userId, final String path) {
        IFile iFile = context.iFactory.findFileByPath(path);
        removeHighlight(userId, path, iFile);
    }

    @Override
    public void removeHighlight(Integer userId, final String path, final IFile file) {
        HashMap<String, LinkedList<RangeHighlighter>> integerRangeHighlighterHashMap = ImpDoc.highlights.get(userId);
        if (file == null) {
            return;
        }
        if (integerRangeHighlighterHashMap == null) {
            return;
        }
        final LinkedList<RangeHighlighter> rangeHighlighters = integerRangeHighlighterHashMap.get(path);
        if (rangeHighlighters == null) {
            return;
        }
        editor.queue(new Runnable() {
            @Override
            public void run() {
                IDoc iDoc = getDocument(file);
                if (iDoc != null) {
                    iDoc.removeHighlight(rangeHighlighters);
                }
            }
        });
    }

    @Override
    public void clearHighlights() {
        HashMap<Integer, HashMap<String, LinkedList<RangeHighlighter>>> highlights = ImpDoc.highlights;
        if (highlights.size() <= 0) {
            return;
        }
        for (Map.Entry<Integer, HashMap<String, LinkedList<RangeHighlighter>>> entry : highlights.entrySet()) {
            HashMap<String, LinkedList<RangeHighlighter>> highlightsForUser = entry.getValue();
            if (highlightsForUser == null || highlightsForUser.size() <= 0) {
                continue;
            }
            Integer user_id = entry.getKey();
            for (Map.Entry<String, LinkedList<RangeHighlighter>> integerLinkedListEntry: highlightsForUser.entrySet()) {
                removeHighlight(user_id, integerLinkedListEntry.getKey());
            }
        }
    }

    @Override
    public IFile findFileByPath(String path) {
        VirtualFile fileByPath = instance.findFileByPath(context.absPath(path));
        if (fileByPath != null && fileByPath.isValid()) {
            return new ImpFile(fileByPath);
        }
        return null;
    }

    @Override
    public IFile findFileByIoFile(File file) {
        VirtualFile fileByIoFile = VfsUtil.findFileByIoFile(file, true);
        if (fileByIoFile == null) {
            return null;
        }
        return new ImpFile(fileByIoFile);
    }

    @Override
    public IDoc getDocument(IFile virtualFile) {
        if (virtualFile == null) {
            return null;
        }
        Document document = FileDocumentManager.getInstance().getDocument(((ImpFile) virtualFile).virtualFile);
        if (document == null) {
            return null;
        }
        return new ImpDoc(context, document);
    }

    @Override
    public IDoc getDocument(String relPath) {
        IFile fileByPath = findFileByPath(context.absPath(relPath));
        return getDocument(fileByPath);
    }

    @Override
    public IFile createDirectories(String path) {
        VirtualFile directory = null;
        try {
            directory = VfsUtil.createDirectories(path);
        } catch (IOException e) {
            Flog.warn(e);
        }

        if (directory == null) {
            Flog.warn("Failed to create directories %s %s", path);
            return null;
        }
        return new ImpFile(directory);
    }
}