# stage 1: build toolchain
FROM ubuntu:18.04 AS toolchain
ENV RISCV=/opt/riscv
ENV TOOLS=/opt/tools
WORKDIR $TOOLS
# Install apt packages
RUN apt-get update && \
    apt-get install -y --no-install-recommends autoconf2.64 automake autogen autotools-dev curl libmpc-dev \
        libmpfr-dev libgmp-dev gawk build-essential bison flex texinfo gcc gperf libtool patchutils \
        bc zlib1g-dev libexpat-dev pkg-config git libusb-1.0-0-dev device-tree-compiler \
        netcat && \
    apt-get clean && rm -rf /var/lib/apt/lists/*
# Install RISC-V GNU toolchain.
COPY riscv-gnu-toolchain $TOOLS
WORKDIR $TOOLS
RUN mkdir -p build && cd build && \
    ../configure --prefix=$RISCV --enable-multilib && \
    make -j$(nproc) && cd / && rm -rf ${TOOLS}

# stage 2: setup
FROM ubuntu:18.04
ENV RISCV=/opt/riscv
ENV PATH=$RISCV/bin:/root/.cargo/bin:$PATH
COPY --from=toolchain $RISCV $RISCV
RUN apt-get update && \
    apt-get install -y --no-install-recommends gnupg curl ca-certificates && \
    echo "deb https://repo.scala-sbt.org/scalasbt/debian all main" | tee -a /etc/apt/sources.list.d/sbt.list && \
    echo "deb https://repo.scala-sbt.org/scalasbt/debian /" | tee /etc/apt/sources.list.d/sbt_old.list && \
    curl -sL "https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x2EE0EA64E40A89B84B2DF73499E82A75642AC823" | apt-key add && \
    apt-get update && \
    apt-get install -y --no-install-recommends verilator git make default-jdk sbt && \
    apt-get clean && rm -rf /var/lib/apt/lists/*
WORKDIR /app
CMD bash
