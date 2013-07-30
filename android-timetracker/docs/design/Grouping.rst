.. sectnum::

========
Grouping
========

:Title: Task grouping feature
:Task: 2c08e
:Rev: 2
:Author: Sean Russell
:Date: 2009-11-22

=== ====== ========== =================================================
Rev Author Date       Note
=== ====== ========== =================================================
1   SER    2009-11-05 Initial draft
2   SER    2009-11-22 Fleshed out the view behavior.  This is good
                      enough for development to start.
=== ====== ========== =================================================


Requirement
===========

Allow a mechanism for grouping tasks together.

Use Case
========

User has several tasks which s/he would like to track independently, but which
are all reported under one PO number.

Cases
-----

The general case is that several tasks may be logically associated with one
another.  This will be called a ``group``.

- User creates a grouping name, and associates existing tasks with this
  grouping.
- User creates a new task, and associates the new task with the grouping.
- User is allowed to specify in reports that the groupings are to be displayed,
  rather than the tasks.
- User can select a view which shows groupings.
- Groupings can be expanded to display the tasks in that group
- Tasks may belong to more than one group
- Totals over a period of time can be reported for a grouping

Exceptions
----------

- A task has no grouping; how does it appear in a report or view?
- A task is in two groups.  How is the group time displayed?  Duplicate the
  task? Only add task's time to first group?
- Filters.  Expanded reporting.


Design
======

One persistent assumption in TimeTracker is that tasks are rarely created or
edited; the common operation is starting/stopping timing, and viewing times.

View / operations
-----------------

Managing tags
~~~~~~~~~~~~~

This should be implicit -- tags should appear in the drop downs, and an option
for a free-text tag should be allowed.  If a tag has no tasks, remove it.  If a
new tag is entered into the free-text field, add it.

Associating tags with tasks
~~~~~~~~~~~~~~~~~~~~~~~~~~~

In addition to editing the task name, the ability to add/remove tags to a task,
similar to adding phone numbers in the contacts app.  Since tasks aren't
composed of tags, this will be an indirection.

Tags in views
~~~~~~~~~~~~~

Add a "filter" concept.  Right now, there's an explicit filter on the time
range, but it isn't generalized.  Change this to a filtering dialog, in which
the user can select the time range and the tags to display.

Add the filter feature to the report view -- although, in that view, the time
filter is useless.  So, allow views to define their own filter sets.

In the report view, also add a "view by tag" option, which will show the tags as
the line entries, with all tasks tagged with the tag being included in the sum
for the tag.

.. Note:
  Would it be useful to add an option that a task appears in a report only once
  -- for example, in the first tag in which it appears, even though it is tagged
  by several?

In the Task view, we can now have several display options: view by tasks, view
by tags, or view as tree.

- For tag view, menu item "View tasks" allows the user to quickly switch to
  "View Tasks" and "Filter on tag" selected.
  - Back button backs up to the tag view
- In the task view, show tags as tiny text
- Tag text everywhere has different color
- As tree, indent tasks under tags.  Allow collapsing.
  - Belay tree view, for now.

Model
-----

Add a new DB table, ``Tags``::

  Tags
  ----
  taskid  foreignkey tasks.id
  tag     string
  
That's it, for the persistence.  This is a many-to-many relationship; a task can
have many tags, and a tag can be associated with many tasks.  A new ``Tag``
class would be in order::

  Tag
  ---
  tasks[] :: int
  name    :: String

This is probably the most useful organization, since tagging tasks is likely to
be less common than organizing tasks by tag.  I could add a ``Tag`` array to
each task, gaining speed at the cost of memory.  I'll save that optimization
until it seems necessary.

Control
-------

Where ``name`` is a ``String`` tag name, and ``task`` is a task ID:

- ``addTag( name )`` : Creates a new tag.
- ``deleteTag( name )`` : Removes a tag, and all task associations
- ``modifyTag( oldname, newname )`` : Alters a tag's name, leaving all
  associations.  Fails with no effect if the target tag name already exists.
- ``mergeTags( oldname, newname )`` : Merges all tasks associated with oldname into
  newname, and removls oldname.
- ``tasks( name )`` : Returns a list of all task IDs associated with a tag.
- ``tags()`` : Returns a list of all tags.
- ``tag( task, name )`` : Associates a task with a tag.
- ``untag( task, name )`` : Removes a task/tag association.
- ``tags( task )`` : Returns a list of all tags associated with a task.
