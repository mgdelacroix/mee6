# Mee6

    Rick: This is a Meeseeks Box, let me show you how it works. You press this...
    Mr. Meeseeks: I'm Mr. Meeseeks! Look at me!
    Rick: You make a request. Mr. Meeseeks, open Jerry's stupid mayonaise jar
    Mr. Meeseeks: Yesiree!
    Rick: The Meeseeks fulfills the request...
    Mr. Meeseeks: All done!
    Rick: ...and then he stops existing.
    [...]
    Rick: Just keep your requests simple. [burps] They're not gods.
-- Rick

## Rationale

`Mee6` is a simple monitoring system. Inspired by Ansible's
simplicity, it is distributed as a `jar` file that only needs the
`JVM` to be run, and it reads a simple `yaml` file where the checks,
hosts and emails to notify are specified.

`Mee6` connects to the remote hosts using plain `ssh`, and those can
be described either in the `Mee6` `yaml` file or in the `ssh` config
file.

Each check uses a module to specify which kind of task it needs to
perform. Every module has its own configuration parameters and its own
way of gathering information and producing output. A check can report
`green`, `red` or `gray` status, meaning `SUCCESS`, `FAILURE` or
`CANNOT CHECK` (failure to perform the check itself) respectively.

At the moment, only `disk-space`, `service` and `script` are
defined. First two perform a specific task, and the latter uploads any
script in the filesystem to the remote host, runs it, saves the exit
code and parses its output to decide what's its status and what info
it should show.

Besides the capability of sending emails to notify of any status
change, it has a simple web interface to visually show the status of
all the checks.

## Installation

## Development
