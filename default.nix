with import <nixpkgs> {};

stdenv.mkDerivation rec {
  name = "mee6";
  src = ./.;
  buildInputs = [
    openjdk
    leiningen
    rlwrap
  ];

  shellHook = ''
    alias front="rlwrap lein frepl"
    alias back="lein brepl"
  '';
}
