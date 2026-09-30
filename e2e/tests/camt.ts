/**
 * Baut camt.054.001.08-Dateien für die E2E-Tests, wie Schweizer Banken sie für QR-Zahlungen liefern. Jede
 * Datei und jede Buchung ist eindeutig, damit Wiederholungen den Duplikatschutz nicht auslösen.
 */
export interface Gutschrift {
  betrag: string
  waehrung?: string
  /** QR-Referenz ohne Leerzeichen oder SCOR-Referenz; ohne Referenz steht die Mitteilung unstrukturiert. */
  referenz?: string
  auftraggeber: string
  mitteilung?: string
}

const QR_IBAN_BERNER_WERKSTATT = 'CH4431999123000889012'

const escape = (text: string) =>
  text.replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;')

export function camt054(gutschriften: Gutschrift[], konto = QR_IBAN_BERNER_WERKSTATT): string {
  const kennung = `E2E-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 8)}`
  const heute = new Date().toISOString().slice(0, 10)
  const buchungen = gutschriften
    .map((gutschrift, index) => {
      const waehrung = gutschrift.waehrung ?? 'CHF'
      const mitteilung = gutschrift.referenz
        ? `<Strd><CdtrRefInf><Tp><CdOrPrtry>${
            gutschrift.referenz.startsWith('RF') ? '<Cd>SCOR</Cd>' : '<Prtry>QRR</Prtry>'
          }</CdOrPrtry></Tp><Ref>${gutschrift.referenz}</Ref></CdtrRefInf></Strd>`
        : `<Ustrd>${escape(gutschrift.mitteilung ?? 'Zahlung')}</Ustrd>`
      return `
      <Ntry>
        <Amt Ccy="${waehrung}">${gutschrift.betrag}</Amt>
        <CdtDbtInd>CRDT</CdtDbtInd>
        <Sts><Cd>BOOK</Cd></Sts>
        <BookgDt><Dt>${heute}</Dt></BookgDt>
        <ValDt><Dt>${heute}</Dt></ValDt>
        <AcctSvcrRef>${kennung}-${index + 1}-B</AcctSvcrRef>
        <BkTxCd><Domn><Cd>PMNT</Cd><Fmly><Cd>RCDT</Cd><SubFmlyCd>VCOM</SubFmlyCd></Fmly></Domn></BkTxCd>
        <NtryDtls>
          <TxDtls>
            <Refs><AcctSvcrRef>${kennung}-${index + 1}</AcctSvcrRef><EndToEndId>NOTPROVIDED</EndToEndId></Refs>
            <Amt Ccy="${waehrung}">${gutschrift.betrag}</Amt>
            <CdtDbtInd>CRDT</CdtDbtInd>
            <RltdPties><Dbtr><Pty><Nm>${escape(gutschrift.auftraggeber)}</Nm></Pty></Dbtr></RltdPties>
            <RmtInf>${mitteilung}</RmtInf>
          </TxDtls>
        </NtryDtls>
      </Ntry>`
    })
    .join('')
  return `<?xml version="1.0" encoding="UTF-8"?>
<Document xmlns="urn:iso:std:iso:20022:tech:xsd:camt.054.001.08">
  <BkToCstmrDbtCdtNtfctn>
    <GrpHdr><MsgId>${kennung}</MsgId><CreDtTm>${new Date().toISOString()}</CreDtTm></GrpHdr>
    <Ntfctn>
      <Id>NTF-${kennung}</Id>
      <Acct><Id><IBAN>${konto}</IBAN></Id></Acct>${buchungen}
    </Ntfctn>
  </BkToCstmrDbtCdtNtfctn>
</Document>
`
}
