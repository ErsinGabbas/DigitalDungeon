package com.example.digitaldungeon.ui.session.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.digitaldungeon.R
import com.example.digitaldungeon.domain.export.SessionMarkdownLabels
import com.example.digitaldungeon.domain.model.SessionEventType

/**
 * Resolves the wording of the Markdown export against the current configuration.
 *
 * The bridge between resources and the pure formatter: everything the document says is
 * read here, inside composition, and handed over as plain strings. That keeps
 * `formatSessionAsMarkdown` free of a `Context` while still producing a log in the
 * language of the app — and the labels follow a language change for free, because a
 * configuration change recomposes the screen that calls this.
 *
 * Not cached behind `remember`: the strings are read from the composition's
 * configuration, so a cache would have to be keyed on it to stay correct, and six
 * resource lookups per recomposition of one screen are not worth that risk.
 *
 * [SessionMarkdownLabels.participantLine] is deliberately read *unformatted*: the
 * resource is a format string, and `stringResource` without arguments returns it with
 * its placeholders intact for the formatter to fill in.
 */
@Composable
fun sessionMarkdownLabels(): SessionMarkdownLabels =
    SessionMarkdownLabels(
        untitledSession = stringResource(R.string.session_export_untitled_session),
        timelineHeading = stringResource(R.string.session_export_timeline_heading),
        emptyTimeline = stringResource(R.string.session_export_empty_timeline),
        participantsLabel = stringResource(R.string.session_export_participants_label),
        participantLine = stringResource(R.string.session_export_participant_line),
        eventTypeNames = SessionEventType.entries.associateWith { type ->
            stringResource(eventTypeLabelRes(type))
        }
    )
