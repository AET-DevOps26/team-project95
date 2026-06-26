{
  description = "Development shell for team-project95";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
  };

  outputs =
    { nixpkgs, ... }:
    let
      system = "x86_64-linux";
      pkgs = import nixpkgs {
        inherit system;
      };

      jdk = pkgs.jdk25;
    in
    {
      devShells.${system}.default = pkgs.mkShell {
        packages = [
          jdk
          pkgs.maven
          pkgs.python312
          pkgs.uv
          pkgs.git
          pkgs.bruno
          pkgs.kubectl
          pkgs.bruno-cli
          pkgs.ripgrep
          pkgs.cacert
          pkgs.stdenv.cc.cc.lib
        ];

        JAVA_HOME = "${jdk}/lib/openjdk";
        LD_LIBRARY_PATH = pkgs.lib.makeLibraryPath [
          pkgs.stdenv.cc.cc.lib
        ];

        shellHook = ''
          export UV_PYTHON_PREFERENCE=only-system
          export UV_PYTHON_DOWNLOADS=never

          echo "Entering team-project95 dev shell"
          echo "Java: $(java -version 2>&1 | head -n 1)"
          echo "Maven: $(mvn -version 2>&1 | head -n 1)"
          echo "Python: $(python3 --version 2>&1)"
          echo "uv: $(uv --version 2>&1)"
          echo "JAVA_HOME=$JAVA_HOME"

          d="$PWD"
          while [ "$d" != "/" ]; do
            if [ -f "$d/.venv/bin/activate" ]; then
              source "$d/.venv/bin/activate"
              break
            fi
            d="$(dirname "$d")"
          done
        '';
      };
    };
}
