package com.aitracker.bugtracker.service;

import com.aitracker.bugtracker.entity.Bug;
import com.aitracker.bugtracker.entity.BugComment;
import com.aitracker.bugtracker.entity.User;
import com.aitracker.bugtracker.repository.BugCommentRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BugCommentService {

    private final BugCommentRepository bugCommentRepository;
    private final BugService bugService;

    public BugCommentService(
            BugCommentRepository bugCommentRepository,
            BugService bugService) {

        this.bugCommentRepository = bugCommentRepository;
        this.bugService = bugService;
    }

    // Add comment
    public BugComment addComment(
            Long bugId,
            User user,
            String commentText) {

        Bug bug = bugService.getBugById(bugId);

        if (commentText == null ||
                commentText.trim().isEmpty()) {

            throw new RuntimeException(
                    "Comment cannot be empty"
            );
        }

        BugComment bugComment = new BugComment();

        bugComment.setBug(bug);
        bugComment.setUser(user);
        bugComment.setComment(commentText.trim());

        return bugCommentRepository.save(bugComment);
    }

    // Get comments of a bug
    public List<BugComment> getCommentsByBug(Long bugId) {

        Bug bug = bugService.getBugById(bugId);

        return bugCommentRepository
                .findByBugOrderByCreatedAtAsc(bug);
    }
    //DeleteComment

    public void deleteComment(Long commentId, User currentUser) {

        BugComment comment = bugCommentRepository.findById(commentId)
                .orElseThrow(() ->
                        new RuntimeException("Comment not found"));

        boolean isAdmin =
                currentUser.getRole()== com.aitracker.bugtracker.entity.Role.ADMIN;

        boolean isOwner =
                comment.getUser().getId().equals(currentUser.getId());

        if (!isAdmin && !isOwner) {
            throw new RuntimeException(
                    "You are not allowed to delete this comment"
            );
        }

        bugCommentRepository.delete(comment);
    }

}