package dev.incusspawn.command;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(
        name = "completion",
        description = "Print shell completion script",
        mixinStandardHelpOptions = true
)
public class CompletionCommand implements Runnable {

    enum Shell { bash, zsh }

    @Parameters(index = "0", description = "Shell type: bash, zsh", arity = "0..1", defaultValue = "bash")
    Shell shell;

    @Option(names = "--install", description = "Print installation instructions instead of the script")
    boolean install;

    @Override
    public void run() {
        if (install) {
            printInstallInstructions();
            return;
        }
        switch (shell) {
            case zsh  -> System.out.println(ZSH_COMPLETION);
            case bash -> System.out.println(BASH_COMPLETION);
        }
    }

    private void printInstallInstructions() {
        System.out.println("""
                # ── Zsh ────────────────────────────────────────────────────────────────────
                # Option A: source directly from your ~/.zshrc
                #   eval "$(isx completion zsh)"
                #
                # Option B: save to a completion file (faster shell startup)
                #   mkdir -p ~/.zsh/completions
                #   isx completion zsh > ~/.zsh/completions/_isx
                #   echo 'fpath=(~/.zsh/completions $fpath)' >> ~/.zshrc
                #   echo 'autoload -Uz compinit && compinit' >> ~/.zshrc
                #
                # ── Bash ────────────────────────────────────────────────────────────────────
                # Option A: source directly from your ~/.bashrc
                #   eval "$(isx completion bash)"
                #
                # Option B: save to a completion file
                #   isx completion bash > ~/.local/share/bash-completion/completions/isx
                """);
    }

    // ── Zsh completion ──────────────────────────────────────────────────────────

    private static final String ZSH_COMPLETION = """
            #compdef isx

            _isx_instances() {
              local -a instances
              instances=(${(f)"$(incus list --format=csv --columns=n 2>/dev/null)"})
              _describe -t instances 'instance' instances
            }

            _isx_templates() {
              local -a templates
              templates=(${(f)"$(incus list --format=csv --columns=n 2>/dev/null | grep '^tpl-')"})
              _describe -t templates 'template' templates
            }

            _isx_branch() {
              _arguments \\
                '(-h --help)'{-h,--help}'[Show help]' \\
                '--from=[Source instance to branch from]:instance:_isx_instances' \\
                '--gui[Enable GUI passthrough (Wayland + GPU + audio)]' \\
                '--airgap[Disable network access (complete isolation)]' \\
                '--proxy-only[Restrict network to host proxy only]' \\
                '--inbox=[Host directory to mount read-only at /home/agentuser/inbox]:directory:_files -/' \\
                '--cpu=[CPU core limit]:number' \\
                '--memory=[Memory limit, e.g. 8GB]:size' \\
                '--disk=[Disk size limit]:size' \\
                '--no-start[Don'"'"'t start the instance after creation]' \\
                '1:new instance name'
            }

            _isx_build() {
              _arguments \\
                '(-h --help)'{-h,--help}'[Show help]' \\
                '--all[Rebuild all defined templates]' \\
                '--missing[Build only templates that don'"'"'t exist yet]' \\
                '--vm[Build as a VM instead of a container]' \\
                '--yes[Skip interactive confirmations]' \\
                '1::template name:_isx_templates'
            }

            _isx_destroy() {
              _arguments \\
                '(-h --help)'{-h,--help}'[Show help]' \\
                '--force[Force destruction, even for templates]' \\
                '1:environment name:_isx_instances'
            }

            _isx_list() {
              _arguments \\
                '(-h --help)'{-h,--help}'[Show help]' \\
                '--plain[Plain text output (no TUI)]'
            }

            _isx_shell() {
              _arguments \\
                '(-h --help)'{-h,--help}'[Show help]' \\
                '1:clone name:_isx_instances'
            }

            _isx_project() {
              local state line; typeset -A opt_args
              _arguments -C \\
                '(-h --help)'{-h,--help}'[Show help]' \\
                '1: :->subcmd' \\
                '*:: :->args'

              local -a _project_subcmds
              _project_subcmds=(
                'create:create a project template from a parent base image'
                'update:update a project template (system packages, git repos, dependencies)'
              )

              case $state in
                subcmd) _describe -t subcmds 'project subcommand' _project_subcmds ;;
                args)
                  case $line[1] in
                    create)
                      _arguments \\
                        '(-h --help)'{-h,--help}'[Show help]' \\
                        '--config=[Path to incus-spawn.yaml]:file:_files' \\
                        '1:project template name' ;;
                    update)
                      _arguments \\
                        '(-h --help)'{-h,--help}'[Show help]' \\
                        '--config=[Path to incus-spawn.yaml]:file:_files' \\
                        '1:project template name:_isx_instances' ;;
                  esac ;;
              esac
            }

            _isx_proxy() {
              _arguments \\
                '(-h --help)'{-h,--help}'[Show help]' \\
                '--port=[MITM TLS proxy port]:port' \\
                '--health-port=[Health check HTTP port]:port'
            }

            _isx_completion() {
              _arguments \\
                '(-h --help)'{-h,--help}'[Show help]' \\
                '--install[Print installation instructions]' \\
                '1::shell:(bash zsh)'
            }

            _isx() {
              local context state state_descr line
              typeset -A opt_args

              _arguments -C \\
                '(-h --help)'{-h,--help}'[Show help and exit]' \\
                '(-V --version)'{-V,--version}'[Show version and exit]' \\
                '1: :->cmd' \\
                '*:: :->args'

              case $state in
                cmd)
                  local -a cmds
                  cmds=(
                    'init:one-time host setup (install Incus, configure auth)'
                    'build:build or rebuild a template image'
                    'project:manage project templates'
                    'branch:create a new instance from an existing one'
                    'shell:open a shell in an existing clone'
                    'list:list all incus-spawn environments'
                    'destroy:destroy a clone environment'
                    'update-all:update all templates (packages, git repos, dependencies)'
                    'proxy:start the MITM authentication proxy'
                    'completion:print shell completion script'
                  )
                  _describe -t commands 'isx command' cmds ;;
                args)
                  local cmd=$line[1]
                  (( CURRENT-- ))
                  shift words
                  case $cmd in
                    branch)     _isx_branch ;;
                    build)      _isx_build ;;
                    destroy)    _isx_destroy ;;
                    list)       _isx_list ;;
                    shell)      _isx_shell ;;
                    project)    _isx_project ;;
                    proxy)      _isx_proxy ;;
                    completion) _isx_completion ;;
                  esac ;;
              esac
            }

            compdef _isx isx
            """;

    // ── Bash completion ─────────────────────────────────────────────────────────

    private static final String BASH_COMPLETION = """
            # bash completion for isx (incus-spawn)

            _isx_list_instances() {
              incus list --format=csv --columns=n 2>/dev/null
            }

            _isx_list_templates() {
              incus list --format=csv --columns=n 2>/dev/null | grep '^tpl-'
            }

            _isx() {
              local cur prev words cword
              _init_completion || return

              local commands="init build project branch shell list destroy update-all proxy completion"

              # Determine which subcommand is active
              local cmd=""
              local i
              for (( i=1; i < cword; i++ )); do
                case "${words[i]}" in
                  init|build|project|branch|shell|list|destroy|update-all|proxy|completion)
                    cmd="${words[i]}"
                    break ;;
                esac
              done

              if [[ -z "$cmd" ]]; then
                # Complete top-level commands and options
                case "$cur" in
                  -*)
                    COMPREPLY=( $(compgen -W "--help --version" -- "$cur") )
                    ;;
                  *)
                    COMPREPLY=( $(compgen -W "$commands" -- "$cur") )
                    ;;
                esac
                return
              fi

              case "$cmd" in
                branch)
                  case "$prev" in
                    --from)
                      COMPREPLY=( $(compgen -W "$(_isx_list_instances)" -- "$cur") )
                      return ;;
                    --inbox)
                      _filedir -d
                      return ;;
                    --cpu|--memory|--disk) return ;;
                  esac
                  COMPREPLY=( $(compgen -W "--help --from --gui --airgap --proxy-only --inbox --cpu --memory --disk --no-start" -- "$cur") )
                  ;;
                build)
                  case "$prev" in
                    build)
                      COMPREPLY=( $(compgen -W "$(_isx_list_templates) --help --all --missing --vm --yes" -- "$cur") )
                      return ;;
                  esac
                  COMPREPLY=( $(compgen -W "--help --all --missing --vm --yes" -- "$cur") )
                  ;;
                destroy)
                  case "$prev" in
                    destroy)
                      COMPREPLY=( $(compgen -W "$(_isx_list_instances) --help --force" -- "$cur") )
                      return ;;
                  esac
                  COMPREPLY=( $(compgen -W "--help --force" -- "$cur") )
                  ;;
                list)
                  COMPREPLY=( $(compgen -W "--help --plain" -- "$cur") )
                  ;;
                shell)
                  case "$prev" in
                    shell)
                      COMPREPLY=( $(compgen -W "$(_isx_list_instances) --help" -- "$cur") )
                      return ;;
                  esac
                  COMPREPLY=( $(compgen -W "--help" -- "$cur") )
                  ;;
                project)
                  local proj_subcmds="create update"
                  local proj_cmd=""
                  local j
                  for (( j=i+1; j < cword; j++ )); do
                    case "${words[j]}" in
                      create|update) proj_cmd="${words[j]}"; break ;;
                    esac
                  done
                  if [[ -z "$proj_cmd" ]]; then
                    COMPREPLY=( $(compgen -W "$proj_subcmds --help" -- "$cur") )
                  else
                    case "$proj_cmd" in
                      create) COMPREPLY=( $(compgen -W "--help --config" -- "$cur") ) ;;
                      update)
                        case "$prev" in
                          update)
                            COMPREPLY=( $(compgen -W "$(_isx_list_instances) --help --config" -- "$cur") )
                            return ;;
                        esac
                        COMPREPLY=( $(compgen -W "--help --config" -- "$cur") )
                        ;;
                    esac
                  fi
                  ;;
                proxy)
                  COMPREPLY=( $(compgen -W "--help --port --health-port" -- "$cur") )
                  ;;
                completion)
                  case "$prev" in
                    completion)
                      COMPREPLY=( $(compgen -W "bash zsh --help --install" -- "$cur") )
                      return ;;
                  esac
                  COMPREPLY=( $(compgen -W "--help --install" -- "$cur") )
                  ;;
                init|update-all)
                  COMPREPLY=( $(compgen -W "--help" -- "$cur") )
                  ;;
              esac
            }

            complete -F _isx isx
            """;
}
